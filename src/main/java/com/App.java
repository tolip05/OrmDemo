package com;

import com.db.EntityDbContext;
import com.db.base.DbContext;
import com.entities.Department;
import com.entities.Employee;

import java.sql.*;

public class App {
    private static final String CONNECTION_STRING = "jdbc:mysql://localhost:3306/soft_uni_simple";
    public static void main(String[] args) throws SQLException, InstantiationException, IllegalAccessException {
        System.out.println("It works");
        Connection connection = getConnection();
        DbContext<Employee>usersDbContext =
                getDbContext(connection, Employee.class);

        usersDbContext.find("first_name LIKE 'P%'")
                .forEach(System.out::println);

        System.out.println("==========");

        usersDbContext.find()
                .forEach(System.out::println);

        System.out.println("==========");

        System.out.println(usersDbContext.findFirst());

        System.out.println("==========");

        System.out.println(usersDbContext.findFirst("first_name LIKE 'M%'"));

        System.out.println(usersDbContext.findById(1));

        Employee employee = new Employee();
        employee.setFirstName("Stamatka");
        employee.setLastName("Peshova");
   //     usersDbContext.persist(employee);
//        DbContext<Department> departmentDbContext =
//                getDbContext(connection,Department.class);
//        departmentDbContext.find().forEach(System.out::println);
        connection.close();
//        String queryString = "SELECT * FROM employees";
//        PreparedStatement preparedStatement =
//                connection.prepareStatement(queryString);
//        ResultSet resultSet = preparedStatement.executeQuery();
//        while (resultSet.next()){
//            System.out.println(resultSet.getString("first_name"));
//        }

    }
    private static <T> DbContext<T> getDbContext(Connection connection
            ,Class<T> klass) throws SQLException {
     return new EntityDbContext<T>(connection,klass);
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(CONNECTION_STRING,"root","1234");
    }
}
