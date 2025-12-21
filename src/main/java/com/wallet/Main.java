package com.wallet;

import com.wallet.database.DatabaseMigrator;

import java.sql.SQLException;


public class Main {
    static void main() {
        DatabaseMigrator.migrate();

    }
}
