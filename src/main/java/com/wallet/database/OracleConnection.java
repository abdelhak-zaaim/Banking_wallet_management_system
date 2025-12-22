package com.wallet.database;

import com.wallet.Exceptin.SQLRuntimeException;
import io.github.cdimascio.dotenv.Dotenv;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.util.logging.Logger;

public class OracleConnection implements DataSource {
    private static final String host = "jdbc:oracle:thin:@db.freesql.com:1521/23ai_34ui2";
    private static final String username = "ABDELHAK_ZAAIM_SCHEMA_YZTTP";


    @Override
    public Connection getConnection() throws SQLException {
        Dotenv dotenv = Dotenv.load();
        String password = dotenv.get("DATABASE_PASSWORD");

        try {
            Connection conn = DriverManager.getConnection(host, username, password);
            return conn;
        } catch (SQLException e) {
            throw new SQLRuntimeException("Failed to connect to Oracle database");
        }
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(host, username, password);
            return conn;
        } catch (SQLException e) {
            throw new SQLRuntimeException("Failed to connect to Oracle database");
        }
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return DriverManager.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        DriverManager.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        DriverManager.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return DriverManager.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("getParentLogger not supported");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("Not a wrapper for " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }
}