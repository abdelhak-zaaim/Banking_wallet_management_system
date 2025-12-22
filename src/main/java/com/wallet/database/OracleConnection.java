package com.wallet.database;

import com.wallet.Exceptin.SQLRuntimeException;
import io.github.cdimascio.dotenv.Dotenv;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.util.logging.Logger;

public class OracleConnection implements DataSource {

    private final String url;
    private final String defaultUser;
    private final String defaultPassword;

    public OracleConnection() {
        try {
            Dotenv dotenv = Dotenv.load();
            String host = dotenv.get("ORACLE_HOST", "localhost");
            String port = dotenv.get("ORACLE_PORT", "1521");
            String service = dotenv.get("ORACLE_SERVICE", "XEPDB1");

            this.url = dotenv.get("ORACLE_URL",
                    "jdbc:oracle:thin:@" + host + ":" + port + "/" + service);
            this.defaultUser = dotenv.get("ORACLE_USER");
            this.defaultPassword = dotenv.get("ORACLE_PASSWORD");

            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLRuntimeException("Oracle JDBC driver not found", e);
        } catch (Exception e) {
            throw new SQLRuntimeException("Failed to initialize OracleConnection", e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (defaultUser == null || defaultPassword == null) {
            throw new SQLRuntimeException("Default DB credentials are not configured");
        }
        return DriverManager.getConnection(url, defaultUser, defaultPassword);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        if (username == null || password == null) {
            throw new SQLRuntimeException("Username and password must not be null");
        }
        return DriverManager.getConnection(url, username, password);
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