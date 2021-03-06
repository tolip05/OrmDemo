package com.db;

import com.annotations.Column;
import com.annotations.Entity;
import com.annotations.PrimaryKey;
import com.db.base.DbContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

public class EntityDbContext<T> implements DbContext<T> {

    private static final String SELECT_QUERY_TEMPLATE = "SELECT * FROM {0}";
    private static final String SELECT_WHERE_QUERY_TEMPLATE = "SELECT * FROM {0} WHERE {1}";
    private static final String SELECT_SINGLE_QUERY_TEMPLATE = "SELECT * FROM {0} LIMIT 1";
    private static final String SELECT_SINGLE_WHERE_QUERY_TEMPLATE = "SELECT * FROM {0} WHERE {1} LIMIT 1";
    private static final String SELECT_BY_PRIMARY_KEY_QUERY_TEMPLATE = "SELECT * FROM {0} WHERE {1}={2}";
    private static final String INSERT_QUERY_TEMPLATE = "INSERT INTO {0}({1}) VALUES({2})";
    private static final String UPDATE_QUERY_TEMPLATE = "UPDATE {0} SET {1} WHERE {2}={3}";
    private static final String SET_QUERY_TEMPLATE = "{0}={1}";
    private static final String WHERE_PRIMARY_KEY = " {0}={1} ";

    private Connection connection;
    private final Class<T> klass;

    public EntityDbContext(Connection connection,Class<T> klass) throws SQLException {
        this.connection = connection;
        this.klass = klass;
        if (this.checksIfDataExists()){
            this.updateTable();
        }else{
            this.createTable();
        }
    }

    public boolean persist(T entity) throws IllegalAccessException, SQLException {
        Field primaryKeyField = getPrimaryKeyField();
        primaryKeyField.setAccessible(true);
        long primaryKey = (long) primaryKeyField.get(entity);
        if (primaryKey > 0){
            return update(entity);
        }
        return insert(entity);
    }

    private boolean insert(T entity) throws SQLException {

        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        getColumnFields().forEach(field ->{
            try {
                field.setAccessible(true);
            String columnName = field.getAnnotation(Column.class).name();

                Object value = field.get(entity);
                columns.add(columnName);
                values.add(value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
        String columnsNames = String.join(", ",columns);

        String columnValues = values.stream()
                .map(value -> {
                    if(value instanceof String){
                        return "\'" + value + "\'";
                    }
                    return value;
                }).map(Object::toString)
                .collect(Collectors.joining(", "));
      String queryString = MessageFormat.format(
              INSERT_QUERY_TEMPLATE,
              getTableName(),
              columnsNames,
              columnValues
      );
      return connection.prepareStatement(queryString).execute();
    }

    private boolean update(T entity) throws SQLException, IllegalAccessException {

        List<String> updateQueries = getColumnFields().stream()
                .map(field ->{
                    field.setAccessible(true);
                    try {
                    String columnName = field.getAnnotation(Column.class).name();

                        Object value = field.get(entity);

                        if(value instanceof String){
                            return "\'" + value + "\'";
                        }
                        return MessageFormat.format(
                                SET_QUERY_TEMPLATE,
                                columnName,
                                value
                        );
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .collect(Collectors.toList());

        String updateQueriesString = String.join(", ",updateQueries);

        Field primaryKey = getPrimaryKeyField();
        primaryKey.setAccessible(true);

        String primaryKeyName = primaryKey
                .getAnnotation(PrimaryKey.class).name();

        long primaryKeyValue = (long) primaryKey.get(entity);

        String queryString = MessageFormat.format(
                UPDATE_QUERY_TEMPLATE,
                getTableName(),
                updateQueriesString,
                primaryKeyName,
                primaryKeyValue
        );
      return connection.prepareStatement(queryString).execute();
    }

    public List<T> find() throws SQLException, IllegalAccessException, InstantiationException {
//        String queryString = MessageFormat
//                .format(SELECT_QUERY_TEMPLATE,getTableName());
//
//        PreparedStatement query =
//                this.connection.prepareStatement(queryString);
//        ResultSet resultSet = query.executeQuery();
//        return toList(resultSet);
        return find(null);
    }




    public List<T> find(String where) throws SQLException, IllegalAccessException, InstantiationException {
        String queryTemplate =
                where == null ? SELECT_QUERY_TEMPLATE : SELECT_WHERE_QUERY_TEMPLATE;


        return find(queryTemplate,where);
    }

    public T findFirst() throws IllegalAccessException, SQLException, InstantiationException {
        return this.findFirst(null);
    }

    public T findFirst(String where) throws SQLException, IllegalAccessException, InstantiationException {
        String queryTemplate = where == null ? SELECT_SINGLE_QUERY_TEMPLATE :
                SELECT_SINGLE_WHERE_QUERY_TEMPLATE;

               return find(queryTemplate,where).get(0);
    }

    public T findById(long id) throws IllegalAccessException, SQLException, InstantiationException {
        String primaryKeyName =
                getPrimaryKeyField().getAnnotation(PrimaryKey.class).name();

        String whereString =
                MessageFormat.format(WHERE_PRIMARY_KEY
        ,primaryKeyName,
                id);
        return findFirst(whereString);
    }

    @Override
    public boolean delete(String where) throws SQLException {
        String query = String.format("DELETE FROM %s WHERE %s"
        ,this.getTableName(),where);
        PreparedStatement preparedStatement =
                this.connection.prepareStatement(query);
        return preparedStatement.execute();
    }

    private String getTableName() {
        //  return this.klass.getSimpleName().toLowerCase() + "s";
        //     Entity annotation = this.klass.getAnnotation(Entity.class);

        Annotation annotation = Arrays.stream(this.klass.getAnnotations())
                .filter(a -> a.getClass() == Entity.class).findFirst().orElse(null);
        if (annotation == null){
            return klass.getSimpleName().toLowerCase() + "s";
        }
        return this.klass.getAnnotation(Entity.class).name();

    }
    private List<T> toList(ResultSet resultSet) throws SQLException, InstantiationException, IllegalAccessException {
        List<T> result = new ArrayList<>();
        while (resultSet.next()){
            T entity = this.createEntity(resultSet);
            result.add(entity);
        }
        return result;
    }

    private T createEntity(ResultSet resultSet) throws IllegalAccessException, InstantiationException, SQLException {
    T entity = klass.newInstance();

        List<Field> columnFields = getColumnFields();

        Field primaryKeyField = getPrimaryKeyField();
        primaryKeyField.setAccessible(true);
        String primaryKeyColumnName = primaryKeyField.getAnnotation(PrimaryKey.class).name();
        long primaryKeyValue = resultSet.getLong(primaryKeyColumnName);
        primaryKeyField.set(entity,primaryKeyValue);

        columnFields.forEach(field ->{
             String columnName = field.getAnnotation(Column.class).name();
             try {
                 field.setAccessible(true);
             if (field.getType() == long.class || field.getType() == Long.class){

                     long value = resultSet.getLong(columnName);

                     field.set(entity,value);
                     }

                     else if(field.getType() == String.class){
                 String value = resultSet.getString(columnName);

                 field.set(entity,value);
             }
                 } catch (SQLException e) {
                     e.printStackTrace();
                 } catch (IllegalAccessException e) {
                     e.printStackTrace();
                 }

         });
        return entity;
    }

    private List<Field> getColumnFields() {
        return Arrays.stream(klass.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Column.class))
                .collect(Collectors.toList());
    }

    private Field getPrimaryKeyField() {
       return Arrays.stream(klass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(PrimaryKey.class))
                .findFirst().orElseThrow(() -> new RuntimeException("Class " + klass + "does not have primary key annotation!"));

    }

    private List<T> find(String template,String where) throws SQLException, IllegalAccessException, InstantiationException {
        String queryString =
                MessageFormat
                        .format(template
                                ,getTableName()
                                ,where);
        PreparedStatement query =
                this.connection.prepareStatement(queryString);
        ResultSet resultSet = query.executeQuery();
        return toList(resultSet);
    }

    private boolean checksIfDataExists() throws SQLException {
      String query = String.format(
              "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '%s'"
      ,this.getTableName());

      PreparedStatement preparedStatement = this.connection.prepareStatement(query);
        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()){
            return true;
        }
        return false;
    }

    private void createTable() throws SQLException {


        Field primaryKeyField = getPrimaryKeyField();

        String primaryKeyColumnName = primaryKeyField.getDeclaredAnnotation(PrimaryKey.class).name();

        String primaryKeyColumnType = this.getColumnTypeString(primaryKeyField);

        String primaryKeyColumnDefinition = String.format
                ("%s %s PRIMARY KEY AUTO_INCREMENT"
                ,primaryKeyColumnName
                ,primaryKeyColumnType);

        List<Field>columnFields = getColumnFields();
        List<String> columnParams = new ArrayList<>();

        columnFields.stream()
                .forEach(field -> {
                    String columnName = field.getDeclaredAnnotation(Column.class).name();
                    String columnType = getColumnTypeString(field);
                    String columnDefinition = String.format("%s %s",columnName,columnType);
                    columnParams.add(columnDefinition);
                });
        String createStatementBody = String.format("%s %s"
        ,primaryKeyColumnDefinition
        ,columnParams.stream().collect(Collectors.joining(", ")));

        String query = String.format("CREATE TABLE %s(%s)"
                ,this.getTableName()
                ,createStatementBody);
        PreparedStatement preparedStatement = this.connection.prepareStatement(query);
        boolean execute = preparedStatement.execute();
        

    }


    private String getColumnTypeString(Field field){
        field.setAccessible(true);
        if (field.getType() == long.class || field.getType() == Long.class || field.getType() == int.class){
            return "INT";
        }else if (field.getType() == String.class){
            return "VARCHAR(255)";
        }
        return null;
    }

    private void updateTable() throws SQLException {
        List<String>entityColumnNames = this.getColumnFields().stream()
                .map(field -> {
                  return field.getDeclaredAnnotation(Column.class).name();
                }).collect(Collectors.toList());
        entityColumnNames.add(this.getPrimaryKeyField().getDeclaredAnnotation(PrimaryKey.class).name());

        List<String> dataBaseColumnNames = this.getDataBaseTableColumnNames();

        List<String>newColumnNames = entityColumnNames.stream()
                .filter(name -> {
                    return !dataBaseColumnNames.contains(name);
                }).collect(Collectors.toList());

        List<Field>newFields = this.getColumnFields()
                .stream().filter(field ->{
                    return newColumnNames.contains(field.getDeclaredAnnotation(Column.class).name());
                }).collect(Collectors.toList());

        List<String>columnDefinitions = new ArrayList<>();

        newFields.stream().forEach(field -> {
            String columnDefinition = String.format("ADD COLUMN %s %s",
                    field.getDeclaredAnnotation(Column.class).name(),
                    this.getColumnTypeString(field));
            columnDefinitions.add(columnDefinition);
        });

        String queryBody = String.join(", ",columnDefinitions);

        String query = String.format("ALTER TABLE %s %s"
        ,this.getTableName(),queryBody);
        PreparedStatement preparedStatement = this.connection.prepareStatement(query);
        preparedStatement.executeQuery();
    }

    private List<String> getDataBaseTableColumnNames() throws SQLException {
        String query = String.format(
                "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME ='%s'"
                ,this.getTableName()
        );

        PreparedStatement preparedStatement = this.connection.prepareStatement(query);
        ResultSet resultSet = preparedStatement.executeQuery();
        List<String>columnNames = new ArrayList<>();
        while (resultSet.next()){
            columnNames.add(resultSet.getString(1));
        }
        return columnNames;
    }
}
