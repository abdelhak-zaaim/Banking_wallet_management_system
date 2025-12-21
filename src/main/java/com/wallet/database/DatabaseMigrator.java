package com.wallet.database;

import io.github.cdimascio.dotenv.Dotenv;
import org.flywaydb.core.Flyway;

public class DatabaseMigrator {

    public static void migrate() {
        try {
            // Make sure the Oracle driver is registered for Flyway
            Class.forName("oracle.jdbc.OracleDriver");

            Dotenv dotenv = Dotenv.load();
            String password = dotenv.get("DATABASE_PASSWORD");

            String url = "jdbc:oracle:thin:@db.freesql.com:1521/23ai_34ui2";
            String user = "ABDELHAK_ZAAIM_SCHEMA_YZTTP";

            Flyway flyway = Flyway.configure()
                    .dataSource(url, user, password)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .baselineVersion("0") // Set baseline to version 0
                    .load();

            flyway.migrate();
        } catch (Exception e) {
            throw new RuntimeException("Flyway migration failed", e);
        }
    }
}