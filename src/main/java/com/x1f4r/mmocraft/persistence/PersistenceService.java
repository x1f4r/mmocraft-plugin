package com.x1f4r.mmocraft.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface PersistenceService {
    Connection getConnection() throws SQLException;
    void initDatabase() throws SQLException;
    void close() throws SQLException;

    <T> Optional<T> executeQuerySingle(String sql, RowMapper<T> mapper, Object... params) throws SQLException;
    <T> List<T> executeQueryList(String sql, RowMapper<T> mapper, Object... params) throws SQLException;
    int executeUpdate(String sql, Object... params) throws SQLException;
}
