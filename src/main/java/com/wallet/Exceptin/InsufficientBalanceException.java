package com.wallet.Exceptin;

public class InsufficientBalanceException extends WalletException {
    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
    }
}