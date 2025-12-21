package com.wallet.transaction;

import com.wallet.Exceptin.SQLRuntimeException;
import com.wallet.database.OracleConnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayDeque;
import java.util.Deque;

public class TransactionManagerImpl implements TransactionManager {
    private Connection conn;
    private final Deque<Savepoint> savepoints = new ArrayDeque<>();


    private TransactionManagerImpl(Connection conn) {
        this.conn = conn;
    }

    private Connection getDatabaseConn() {
        return conn;
    }

    public static TransactionManager startTransaction(TranIsoLevel isoLevel) throws SQLException {
        Connection conn = OracleConnection.getConnection();

        conn.setAutoCommit(false);
        conn.setTransactionIsolation(isoLevel.getLevel());

        return new TransactionManagerImpl(conn);
    }

    @Override
    public void stopTransaction() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    @Override
    public void commitTransaction() throws SQLException {
        if (conn == null) {
            throw new IllegalStateException("Database Transaction is not initialized");
        }
        conn.commit();
        savepoints.clear();
    }

    @Override
    public void rollbackTransaction() throws SQLException {
        if (conn == null) {
            throw new IllegalStateException("Database Transaction is not initialized");
        }
        conn.rollback();
        savepoints.clear();
    }

    @Override
    public TranIsoLevel getTransactionIsoLevel() {
        try {
            return TranIsoLevel.getTranIsoLevel(conn.getTransactionIsolation());
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    @Override
    public void startChildTransaction() throws SQLException {
        if (conn == null) {
            throw new IllegalStateException("Database Transaction is not initialized");
        }
        savepoints.push(conn.setSavepoint());
    }

    @Override
    public void commitChildTransaction() throws SQLException {
        if (savepoints.isEmpty()) {
            throw new IllegalStateException("No child transaction to commit");
        }
        // In JDBC, "committing" a savepoint means releasing it.
        // The changes are actually committed with the parent transaction.
        conn.releaseSavepoint(savepoints.pop());
    }

    @Override
    public void rollbackChildTransaction() throws SQLException {
        if (savepoints.isEmpty()) {
            throw new IllegalStateException("No child transaction to roll back");
        }
        conn.rollback(savepoints.pop());
    }
}
