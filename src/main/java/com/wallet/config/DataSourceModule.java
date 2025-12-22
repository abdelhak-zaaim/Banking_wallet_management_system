package com.wallet.config;

import com.google.inject.AbstractModule;
import io.github.cdimascio.dotenv.Dotenv;
import oracle.jdbc.pool.OracleDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;

public class DataSourceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(DataSource.class).toInstance(createOracleDataSource());
    }

    private DataSource createOracleDataSource() {
        try {
            Dotenv dotenv = Dotenv.load();

            String host = dotenv.get("ORACLE_HOST", "localhost");
            String port = dotenv.get("ORACLE_PORT", "1521");
            String service = dotenv.get("ORACLE_SERVICE", "XEPDB1");

            String url = dotenv.get(
                    "ORACLE_URL",
                    "jdbc:oracle:thin:@" + host + ":" + port + "/" + service
            );

            String user = dotenv.get("ORACLE_USER");
            String password = dotenv.get("ORACLE_PASSWORD");

            if (user == null || password == null) {
                throw new IllegalStateException("ORACLE_USER and ORACLE_PASSWORD must be set in environment");
            }

            OracleDataSource ods = new OracleDataSource();
            ods.setURL(url);
            ods.setUser(user);
            ods.setPassword(password);

            ods.setImplicitCachingEnabled(true);

            return ods;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create OracleDataSource", e);
        }
    }
}