package com.wallet;

import com.wallet.database.DatabaseMigrator;

public class Main {
    static void main() {
        DatabaseMigrator.migrate();

    }
}
