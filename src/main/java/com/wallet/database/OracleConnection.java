package com.wallet.database;

import com.wallet.Exceptin.SQLRuntimeException;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.*;

public class OracleConnection {
    private static final String host = "jdbc:oracle:thin:@db.freesql.com:1521/23ai_34ui2";
    private static final String username = "ABDELHAK_ZAAIM_SCHEMA_YZTTP";

    public static Connection getConnection(){
        Dotenv dotenv = Dotenv.load();
        String password = dotenv.get("DATABASE_PASSWORD");

        try {
            Connection conn = DriverManager.getConnection(host, username, password);
            return conn;
        } catch (SQLException e) {
            throw new SQLRuntimeException("Failed to connect to Oracle database");
        }
    }
}