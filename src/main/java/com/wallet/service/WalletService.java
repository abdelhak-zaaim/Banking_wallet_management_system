package com.wallet.service;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.wallet.Exceptin.InsufficientBalanceException;
import com.wallet.Exceptin.SQLRuntimeException;
import com.wallet.Exceptin.WalletException;
import com.wallet.database.util.SqlTemplate;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;

public class WalletService {

    private final SqlTemplate sqlTemplate;

    @Inject
    public WalletService(SqlTemplate sqlTemplate) {
        this.sqlTemplate = sqlTemplate;
    }

    public String transfer(long fromAccountId, long toAccountId, String currency, double amount) {
        String requestId = UUID.randomUUID().toString();
        return transfer(requestId, fromAccountId, toAccountId, currency, amount);
    }

    public String transfer(String requestId, long fromAccountId, long toAccountId,
                           String currency, double amount) {

        @Language("SQL")
        String sql = "{call wallet_pkg.transfer(?, ?, ?, ?, ?)}";
        try {
            sqlTemplate.callProcedure(sql, requestId, fromAccountId, toAccountId, currency, amount);
        } catch (SQLRuntimeException e) {
            if (e.getCause() != null && e.getCause() instanceof SQLException)
                throw mapSqlException((SQLException) e.getCause());
            else
                throw new WalletException("Unknown Error", e);
        }

        return requestId;
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