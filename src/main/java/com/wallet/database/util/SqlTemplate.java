package com.wallet.database.util;

import com.google.inject.Inject;
import com.wallet.Exceptin.SQLRuntimeException;
import org.intellij.lang.annotations.Language;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * SQL utility class for managing database operations.
 * Provides fluent API for SELECT, INSERT, UPDATE, DELETE operations
 * with proper resource management and exception handling.
 */
public class SqlTemplate {

    private final DataSource dataSource;

    @Inject
    public SqlTemplate(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Execute a SELECT query and map results to a list of objects.
     *
     * @param sql    The SQL query
     * @param mapper Function to map ResultSet row to object
     * @param params Query parameters
     * @param <T>    Type of result object
     * @return List of mapped objects
     */
    public <T> List<T> select(@Language("SQL")  String sql, RowMapper<T> mapper, Object... params) {
        List<T> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = prepareStatement(conn, sql, params);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                results.add(mapper.map(rs));
            }
        } catch (SQLException e) {
            throw new SQLRuntimeException("SELECT query failed: " + sql, e);
        }
        return results;
    }

    /**
     * Execute a SELECT query and return a single result.
     *
     * @param sql    The SQL query
     * @param mapper Function to map ResultSet row to object
     * @param params Query parameters
     * @param <T>    Type of result object
     * @return Optional containing the result or empty if no rows
     */
    public <T> Optional<T> selectOne(@Language("SQL")  String sql, RowMapper<T> mapper, Object... params) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = prepareStatement(conn, sql, params);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return Optional.ofNullable(mapper.map(rs));
            }
        } catch (SQLException e) {
            throw new SQLRuntimeException("SELECT query failed: " + sql, e);
        }
        return Optional.empty();
    }

    /**
     * Execute a SELECT query and return the first column as a single value.
     *
     * @param sql    The SQL query
     * @param type   Expected type of the result
     * @param params Query parameters
     * @param <T>    Type of result
     * @return Optional containing the value or empty if no rows
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> selectScalar(@Language("SQL")  String sql, Class<T> type, Object... params) {
        return selectOne(sql, rs -> (T) rs.getObject(1), params);
    }

    /**
     * Execute a COUNT query and return the result.
     *
     * @param tableName Table to count from
     * @param where     Optional WHERE clause (without 'WHERE' keyword)
     * @param params    Query parameters for WHERE clause
     * @return Count of matching rows
     */
    public long count(String tableName, String where, Object... params) {
        @Language("SQL")
        String sql = "SELECT COUNT(*) FROM " + tableName;
        if (where != null && !where.isBlank()) {
            sql += " WHERE " + where;
        }
        return selectScalar(sql, Number.class, params)
                .map(Number::longValue)
                .orElse(0L);
    }

    /**
     * Check if any rows exist matching the criteria.
     *
     * @param tableName Table to check
     * @param where     WHERE clause (without 'WHERE' keyword)
     * @param params    Query parameters
     * @return true if at least one row exists
     */
    public boolean exists(String tableName, String where, Object... params) {
        @Language("SQL")
        String sql = "SELECT 1 FROM " + tableName + " WHERE " + where + " FETCH FIRST 1 ROWS ONLY";
        return selectScalar(sql, Object.class, params).isPresent();
    }

    /**
     * Execute an INSERT statement.
     *
     * @param sql    The INSERT SQL
     * @param params Insert parameters
     * @return Number of rows inserted
     */
    public int insert(@Language("SQL")  String sql, Object... params) {
        return executeUpdate(sql, params);
    }

    /**
     * Insert a row and return the generated key.
     *
     * @param sql        The INSERT SQL
     * @param keyColumn  Name of the generated key column
     * @param params     Insert parameters
     * @return Generated key value
     */
    public Optional<Long> insertAndGetKey(@Language("SQL")  String sql, String keyColumn, Object... params) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, new String[]{keyColumn})) {

            setParameters(stmt, params);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return Optional.of(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new SQLRuntimeException("INSERT with key generation failed: " + sql, e);
        }
        return Optional.empty();
    }

    /**
     * Batch insert multiple rows.
     *
     * @param sql        The INSERT SQL with placeholders
     * @param batchData  List of parameter arrays for each row
     * @return Array of update counts
     */
    public int[] batchInsert(@Language("SQL")  String sql, List<Object[]> batchData) {
        return executeBatch(sql, batchData);
    }

    /**
     * Execute an UPDATE statement.
     *
     * @param sql    The UPDATE SQL
     * @param params Update parameters
     * @return Number of rows updated
     */
    public int update(@Language("SQL")  String sql, Object... params) {
        return executeUpdate(sql, params);
    }

    /**
     * Batch update multiple rows.
     *
     * @param sql        The UPDATE SQL with placeholders
     * @param batchData  List of parameter arrays for each update
     * @return Array of update counts
     */
    public int[] batchUpdate(@Language("SQL")  String sql, List<Object[]> batchData) {
        return executeBatch(sql, batchData);
    }

    /**
     * Execute a DELETE statement.
     *
     * @param sql    The DELETE SQL
     * @param params Delete parameters
     * @return Number of rows deleted
     */
    public int delete(@Language("SQL")  String sql, Object... params) {
        return executeUpdate(sql, params);
    }

    /**
     * Delete rows from a table by criteria.
     *
     * @param tableName Table to delete from
     * @param where     WHERE clause (without 'WHERE' keyword)
     * @param params    Query parameters
     * @return Number of rows deleted
     */
    public int deleteWhere(String tableName, String where, Object... params) {
        @Language("SQL")
        String sql = "DELETE FROM " + tableName + " WHERE " + where;
        return executeUpdate(sql, params);
    }

    /**
     * Execute multiple operations within a transaction.
     *
     * @param operations Function that performs database operations
     * @param <T>        Type of result
     * @return Result of the operations
     */
    public <T> T executeInTransaction(TransactionCallback<T> operations) {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            T result = operations.execute(new TransactionContext(conn));
            conn.commit();
            return result;

        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new SQLRuntimeException("Transaction failed", e);
        } finally {
            closeQuietly(conn);
        }
    }

    /**
     * Execute operations within a transaction without returning a result.
     *
     * @param operations Runnable that performs database operations
     */
    public void executeInTransactionVoid(TransactionVoidCallback operations) {
        executeInTransaction(ctx -> {
            operations.execute(ctx);
            return null;
        });
    }

    /**
     * Call a stored procedure.
     *
     * @param procedureCall The procedure call (e.g., "{call my_proc(?, ?)}")
     * @param params        Procedure parameters
     */
    public void callProcedure(String procedureCall, Object... params) {
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall(procedureCall)) {

            setParameters(stmt, params);
            stmt.execute();

        } catch (SQLException e) {
            throw new SQLRuntimeException("Procedure call failed: " + procedureCall, e);
        }
    }

    /**
     * Call a stored procedure with OUT parameters.
     *
     * @param procedureCall The procedure call
     * @param outParams     Map of OUT parameter positions to SQL types
     * @param inParams      IN parameters
     * @return Map of OUT parameter positions to values
     */
    public Map<Integer, Object> callProcedureWithOut(String procedureCall,
                                                      Map<Integer, Integer> outParams,
                                                      Object... inParams) {
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall(procedureCall)) {

            // Register OUT parameters
            for (Map.Entry<Integer, Integer> entry : outParams.entrySet()) {
                stmt.registerOutParameter(entry.getKey(), entry.getValue());
            }

            // Set IN parameters
            setParameters(stmt, inParams);
            stmt.execute();

            // Collect OUT parameter values
            Map<Integer, Object> results = new HashMap<>();
            for (Integer pos : outParams.keySet()) {
                results.put(pos, stmt.getObject(pos));
            }
            return results;

        } catch (SQLException e) {
            throw new SQLRuntimeException("Procedure call failed: " + procedureCall, e);
        }
    }

    /**
     * Create a new SELECT query builder.
     *
     * @return SelectBuilder instance
     */
    public SelectBuilder selectBuilder() {
        return new SelectBuilder(this);
    }

    private int executeUpdate(@Language("SQL")  String sql, Object... params) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = prepareStatement(conn, sql, params)) {
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new SQLRuntimeException("Execute update failed: " + sql, e);
        }
    }

    private int[] executeBatch(@Language("SQL")  String sql, List<Object[]> batchData) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (Object[] params : batchData) {
                setParameters(stmt, params);
                stmt.addBatch();
            }
            return stmt.executeBatch();

        } catch (SQLException e) {
            throw new SQLRuntimeException("Batch execution failed: " + sql, e);
        }
    }

    private PreparedStatement prepareStatement(Connection conn, @Language("SQL")  String sql, Object... params)
            throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(sql);
        setParameters(stmt, params);
        return stmt;
    }

    private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            setParameter(stmt, i + 1, params[i]);
        }
    }

    private void setParameter(PreparedStatement stmt, int index, Object value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.NULL);
        } else if (value instanceof String) {
            stmt.setString(index, (String) value);
        } else if (value instanceof Integer) {
            stmt.setInt(index, (Integer) value);
        } else if (value instanceof Long) {
            stmt.setLong(index, (Long) value);
        } else if (value instanceof Double) {
            stmt.setDouble(index, (Double) value);
        } else if (value instanceof Float) {
            stmt.setFloat(index, (Float) value);
        } else if (value instanceof Boolean) {
            stmt.setBoolean(index, (Boolean) value);
        } else if (value instanceof Timestamp) {
            stmt.setTimestamp(index, (Timestamp) value);
        } else if (value instanceof java.sql.Date) {
            stmt.setDate(index, (java.sql.Date) value);
        } else if (value instanceof java.util.Date) {
            stmt.setTimestamp(index, new Timestamp(((java.util.Date) value).getTime()));
        } else if (value instanceof byte[]) {
            stmt.setBytes(index, (byte[]) value);
        } else {
            stmt.setObject(index, value);
        }
    }

    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            }
        }
    }

    private void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception ignored) {
            }
        }
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    @FunctionalInterface
    public interface TransactionCallback<T> {
        T execute(TransactionContext ctx) throws SQLException;
    }

    @FunctionalInterface
    public interface TransactionVoidCallback {
        void execute(TransactionContext ctx) throws SQLException;
    }

    public class TransactionContext {
        private final Connection connection;

        TransactionContext(Connection connection) {
            this.connection = connection;
        }

        public <T> List<T> select(@Language("SQL")  String sql, RowMapper<T> mapper, Object... params) throws SQLException {
            List<T> results = new ArrayList<>();
            try (PreparedStatement stmt = prepareStatement(connection, sql, params);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
            }
            return results;
        }

        public <T> Optional<T> selectOne(@Language("SQL")  String sql, RowMapper<T> mapper, Object... params) throws SQLException {
            try (PreparedStatement stmt = prepareStatement(connection, sql, params);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(mapper.map(rs));
                }
            }
            return Optional.empty();
        }

        public int insert(@Language("SQL")  String sql, Object... params) throws SQLException {
            return executeUpdateInTx(sql, params);
        }

        public int update(@Language("SQL")  String sql, Object... params) throws SQLException {
            return executeUpdateInTx(sql, params);
        }

        public int delete(@Language("SQL")  String sql, Object... params) throws SQLException {
            return executeUpdateInTx(sql, params);
        }

        private int executeUpdateInTx(@Language("SQL")  String sql, Object... params) throws SQLException {
            try (PreparedStatement stmt = prepareStatement(connection, sql, params)) {
                return stmt.executeUpdate();
            }
        }

        public Connection getConnection() {
            return connection;
        }
    }

    public static class SelectBuilder {
        private final SqlTemplate sqlTemplate;
        private final List<String> columns = new ArrayList<>();
        private String table;
        private final List<String> joins = new ArrayList<>();
        private final List<String> conditions = new ArrayList<>();
        private final List<Object> parameters = new ArrayList<>();
        private final List<String> groupBy = new ArrayList<>();
        private String having;
        private final List<String> orderBy = new ArrayList<>();
        private Integer limit;
        private Integer offset;

        SelectBuilder(SqlTemplate sqlTemplate) {
            this.sqlTemplate = sqlTemplate;
        }

        public SelectBuilder columns(String... cols) {
            columns.addAll(Arrays.asList(cols));
            return this;
        }

        public SelectBuilder from(String tableName) {
            this.table = tableName;
            return this;
        }

        public SelectBuilder innerJoin(String joinClause) {
            joins.add("INNER JOIN " + joinClause);
            return this;
        }

        public SelectBuilder leftJoin(String joinClause) {
            joins.add("LEFT JOIN " + joinClause);
            return this;
        }

        public SelectBuilder rightJoin(String joinClause) {
            joins.add("RIGHT JOIN " + joinClause);
            return this;
        }

        public SelectBuilder where(String condition, Object... params) {
            conditions.add(condition);
            parameters.addAll(Arrays.asList(params));
            return this;
        }

        public SelectBuilder and(String condition, Object... params) {
            return where(condition, params);
        }

        public SelectBuilder groupBy(String... cols) {
            groupBy.addAll(Arrays.asList(cols));
            return this;
        }

        public SelectBuilder having(String havingClause) {
            this.having = havingClause;
            return this;
        }

        public SelectBuilder orderBy(String... cols) {
            orderBy.addAll(Arrays.asList(cols));
            return this;
        }

        public SelectBuilder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public SelectBuilder offset(int offset) {
            this.offset = offset;
            return this;
        }

        @Language("SQL")
        public String buildSql() {
            StringBuilder sql = new StringBuilder("SELECT ");
            sql.append(columns.isEmpty() ? "*" : String.join(", ", columns));
            sql.append(" FROM ").append(table);

            for (String join : joins) {
                sql.append(" ").append(join);
            }

            if (!conditions.isEmpty()) {
                sql.append(" WHERE ").append(String.join(" AND ", conditions));
            }

            if (!groupBy.isEmpty()) {
                sql.append(" GROUP BY ").append(String.join(", ", groupBy));
            }

            if (having != null) {
                sql.append(" HAVING ").append(having);
            }

            if (!orderBy.isEmpty()) {
                sql.append(" ORDER BY ").append(String.join(", ", orderBy));
            }

            // Oracle-style pagination
            if (offset != null && limit != null) {
                sql.append(" OFFSET ").append(offset).append(" ROWS FETCH NEXT ").append(limit).append(" ROWS ONLY");
            } else if (limit != null) {
                sql.append(" FETCH FIRST ").append(limit).append(" ROWS ONLY");
            }

            return sql.toString();
        }

        public <T> List<T> execute(RowMapper<T> mapper) {
            return sqlTemplate.select(buildSql(), mapper, parameters.toArray());
        }

        public <T> Optional<T> executeOne(RowMapper<T> mapper) {
            return sqlTemplate.selectOne(buildSql(), mapper, parameters.toArray());
        }

        public long count() {
            @Language("SQL")
            String countSql = "SELECT COUNT(*) FROM " + table;
            if (!conditions.isEmpty()) {
                countSql += " WHERE " + String.join(" AND ", conditions);
            }
            return sqlTemplate.selectScalar(countSql, Number.class, parameters.toArray())
                    .map(Number::longValue)
                    .orElse(0L);
        }
    }
}
