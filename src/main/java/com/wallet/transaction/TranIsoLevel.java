package com.wallet.transaction;

import java.sql.Connection;

public enum TranIsoLevel {
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);

    private final int level;

    TranIsoLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

   public static TranIsoLevel getTranIsoLevel(int level) {
       switch (level) {
           case Connection.TRANSACTION_READ_UNCOMMITTED:
               return READ_UNCOMMITTED;
           case Connection.TRANSACTION_READ_COMMITTED:
               return READ_COMMITTED;
           case Connection.TRANSACTION_REPEATABLE_READ:
               return REPEATABLE_READ;
           case Connection.TRANSACTION_SERIALIZABLE:
               return SERIALIZABLE;
           default:
               throw new IllegalArgumentException("Invalid transaction isolation level: " + level);
       }
   }
}