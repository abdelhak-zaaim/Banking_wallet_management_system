package com.wallet.service;

import com.google.inject.Inject;
import com.wallet.Exceptin.InsufficientBalanceException;
import com.wallet.Exceptin.WalletException;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;

public class WalletService {

    private final DataSource dataSource;

    @Inject
    public WalletService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String transfer(long fromAccountId, long toAccountId, String currency, double amount) {
        String requestId = UUID.randomUUID().toString();
        return transfer(requestId, fromAccountId, toAccountId, currency, amount);
    }

    public String transfer(String requestId, long fromAccountId, long toAccountId,
                           String currency, double amount) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            try (CallableStatement stmt = conn.prepareCall(
                    "{call wallet_pkg.transfer(?, ?, ?, ?, ?)}")) {

                stmt.setString(1, requestId);
                stmt.setLong(2, fromAccountId);
                stmt.setLong(3, toAccountId);
                stmt.setString(4, currency);
                stmt.setDouble(5, amount);

                stmt.execute();
                conn.commit();
                return requestId;

            } catch (SQLException e) {
                conn.rollback();
                throw mapSqlException(e);
            }
        } catch (SQLException e) {
            throw new WalletException("Database connection error", e);
        }
    }

    private WalletException mapSqlException(SQLException e) {
        int errorCode = e.getErrorCode();
        return switch (errorCode) {
            case 20003 -> new WalletException("Source and destination accounts must differ", e);
            case 20004 -> new WalletException("Invalid amount", e);
            case 20005 -> new WalletException("Account not found", e);
            case 20006 -> new InsufficientBalanceException("Insufficient balance", e);
            case 20007 -> new WalletException("Account is not active", e);
            case 20008 -> new WalletException("Currency mismatch", e);
            case 20009 -> new WalletException("Duplicate request", e);
            default -> new WalletException("Transfer failed: " + e.getMessage(), e);
        };
    }
}