package com.wallet.transaction;

import com.wallet.Exceptin.SQLRuntimeException;
import com.wallet.database.OracleConnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TransactionManagerImpl implements TransactionManager {
    private Connection conn;

    private TransactionManagerImpl(Connection conn) {
        this.conn = conn;
    }

    private Connection getDatabaseConn() {
        return conn;
    }

    public static TransactionManager startTransaction(TranIsoLevel isoLevel) {
        Connection conn = OracleConnection.getConnection();
        try {
            conn.setTransactionIsolation(isoLevel.getLevel());
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
        return new TransactionManagerImpl(conn);
    }

    @Override
    public void stopTransaction() {
        try {
            conn.close();
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    @Override
    public void commitTransaction() throws SQLException {
        if (conn == null) {
            throw new RuntimeException("Database Transaction is not initialized");
        }
        conn.commit();
    }

    @Override
    public void rollbackTransaction() throws SQLException {
        if (conn == null) {
            throw new RuntimeException("Database Transaction is not initialized");
        }
        conn.rollback();
    }

    @Override
    public TranIsoLevel getTransactionIsoLevel() {
        try {
            return TranIsoLevel.getTranIsoLevel(conn.getTransactionIsolation());
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }
}
