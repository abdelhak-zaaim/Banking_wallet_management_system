package com.wallet.transaction;

import java.sql.SQLException;

/**
 * this transaction manager will be responsible for starting a transaction
 * provide database connection, ensure that the transaction finalized correctly
 * <p>
 * the foundation of this is a database connection
 */
public interface TransactionManager {

    void stopTransaction() throws SQLException;

    void commitTransaction() throws SQLException;

    void rollbackTransaction() throws SQLException;

    TranIsoLevel getTransactionIsoLevel() throws SQLException;

    void startChildTransaction() throws SQLException;

    public void commitChildTransaction() throws SQLException;

    void rollbackChildTransaction() throws SQLException;
}
