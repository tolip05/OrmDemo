package com.db.base;

import java.sql.SQLException;
import java.util.List;

public interface DbContext<T> {
    boolean persist(T entity) throws IllegalAccessException, SQLException;

    List<T> find() throws SQLException, IllegalAccessException, InstantiationException;

    List<T> find(String where) throws SQLException, IllegalAccessException, InstantiationException;

    T findFirst() throws IllegalAccessException, SQLException, InstantiationException;

    T findFirst(String where) throws SQLException, IllegalAccessException, InstantiationException;

    T findById(long id) throws IllegalAccessException, SQLException, InstantiationException;

    boolean delete(String where) throws SQLException;
}
