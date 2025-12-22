package com.wallet.Exceptin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionsTest {

    @Test
    void walletException_constructors() {
        WalletException ex1 = new WalletException("msg");
        assertEquals("msg", ex1.getMessage());

        Throwable cause = new RuntimeException("root");
        WalletException ex2 = new WalletException("wrap", cause);
        assertEquals("wrap", ex2.getMessage());
        assertSame(cause, ex2.getCause());
    }

    @Test
    void insufficientBalanceException_is_walletException() {
        Throwable cause = new RuntimeException("root");
        InsufficientBalanceException ex = new InsufficientBalanceException("low", cause);
        WalletException asWallet = ex; // upcast
        assertEquals("low", asWallet.getMessage());
        assertSame(cause, asWallet.getCause());
        assertTrue(asWallet instanceof WalletException);
    }
}

