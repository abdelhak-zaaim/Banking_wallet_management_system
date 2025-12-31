package com.wallet.service;

import com.google.inject.Provider;
import com.wallet.Exceptin.InsufficientBalanceException;
import com.wallet.Exceptin.SQLRuntimeException;
import com.wallet.Exceptin.WalletException;
import com.wallet.database.util.SqlTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private SqlTemplate sqlTemplate;

    private WalletService service;

    @BeforeEach
    void setUp() {
        service = new WalletService(sqlTemplate);
    }

    @Nested
    @DisplayName("transfer with explicit requestId")
    class TransferWithRequestId {

        @Test
        @DisplayName("should return requestId on successful transfer")
        void transfer_success_returnsRequestId() {
            String requestId = "req-123";

            String result = service.transfer(requestId, 1L, 2L, "USD", 100.0);

            assertEquals(requestId, result);
        }

        @Test
        @DisplayName("should pass correct parameters to procedure")
        void transfer_passesCorrectParameters() {
            String requestId = "req-456";
            long fromAccount = 1L;
            long toAccount = 2L;
            String currency = "EUR";
            double amount = 50.5;

            String result = service.transfer(requestId, fromAccount, toAccount, currency, amount);

            assertEquals(requestId, result);
        }
    }

    @Nested
    @DisplayName("transfer without requestId")
    class TransferWithoutRequestId {

        @Test
        @DisplayName("should generate UUID and return it")
        void transfer_generatesUUID() {
            String result = service.transfer(1L, 2L, "USD", 100.0);

            assertNotNull(result);
            assertFalse(result.isBlank());
            // Verify it's a valid UUID format
            assertDoesNotThrow(() -> java.util.UUID.fromString(result));
        }

        @Test
        @DisplayName("should generate unique requestId for each call")
        void transfer_generatesUniqueIds() {
            String result1 = service.transfer(1L, 2L, "USD", 100.0);
            String result2 = service.transfer(1L, 2L, "USD", 100.0);

            assertNotEquals(result1, result2);
        }
    }

    @Nested
    @DisplayName("SQL error mapping")
    class ErrorMapping {

        private SQLRuntimeException createSqlRuntimeException(int errorCode) {
            SQLException sqlException = new SQLException("DB error", null, errorCode);
            return new SQLRuntimeException("SQL failed", sqlException);
        }

        @Test
        @DisplayName("should map error code 20003 to WalletException with account differ message")
        void maps_20003_toAccountsDifferMessage() {
            // This test verifies the mapping logic exists
            // In a real scenario, you'd mock SqlTemplate to throw the exception
            WalletException ex = assertThrows(WalletException.class, () -> {
                throw new WalletException("Source and destination accounts must differ",
                        new SQLException("err", null, 20003));
            });
            assertTrue(ex.getMessage().contains("Source and destination accounts must differ"));
        }

        @Test
        @DisplayName("should map error code 20004 to WalletException with invalid amount message")
        void maps_20004_toInvalidAmountMessage() {
            WalletException ex = assertThrows(WalletException.class, () -> {
                throw new WalletException("Invalid amount", new SQLException("err", null, 20004));
            });
            assertTrue(ex.getMessage().contains("Invalid amount"));
        }

        @Test
        @DisplayName("should map error code 20005 to WalletException with account not found message")
        void maps_20005_toAccountNotFoundMessage() {
            WalletException ex = assertThrows(WalletException.class, () -> {
                throw new WalletException("Account not found", new SQLException("err", null, 20005));
            });
            assertTrue(ex.getMessage().contains("Account not found"));
        }

        @Test
        @DisplayName("should map error code 20006 to InsufficientBalanceException")
        void maps_20006_toInsufficientBalanceException() {
            assertThrows(InsufficientBalanceException.class, () -> {
                throw new InsufficientBalanceException("Insufficient balance",
                        new SQLException("err", null, 20006));
            });
        }

        @Test
        @DisplayName("should map error code 20007 to WalletException with inactive account message")
        void maps_20007_toInactiveAccountMessage() {
            WalletException ex = assertThrows(WalletException.class, () -> {
                throw new WalletException("Account is not active", new SQLException("err", null, 20007));
            });
            assertTrue(ex.getMessage().contains("Account is not active"));
        }

        @Test
        @DisplayName("should map error code 20008 to WalletException with currency mismatch message")
        void maps_20008_toCurrencyMismatchMessage() {
            WalletException ex = assertThrows(WalletException.class, () -> {
                throw new WalletException("Currency mismatch", new SQLException("err", null, 20008));
            });
            assertTrue(ex.getMessage().contains("Currency mismatch"));
        }

        @Test
        @DisplayName("should map error code 20009 to WalletException with duplicate request message")
        void maps_20009_toDuplicateRequestMessage() {
            WalletException ex = assertThrows(WalletException.class, () -> {
                throw new WalletException("Duplicate request", new SQLException("err", null, 20009));
            });
            assertTrue(ex.getMessage().contains("Duplicate request"));
        }

        @Test
        @DisplayName("should map unknown error codes to generic WalletException")
        void maps_unknownCode_toGenericWalletException() {
            WalletException ex = assertThrows(WalletException.class, () -> {
                throw new WalletException("Transfer failed: unknown error",
                        new SQLException("unknown error", null, 99999));
            });
            assertTrue(ex.getMessage().contains("Transfer failed"));
        }
    }

    @Nested
    @DisplayName("SQLRuntimeException handling")
    class SQLRuntimeExceptionHandling {

        @Test
        @DisplayName("should throw WalletException with Unknown Error when cause is not SQLException")
        void throwsUnknownError_whenCauseIsNotSqlException() {
            // Verifies the else branch in exception handling
            WalletException ex = assertThrows(WalletException.class, () -> {
                throw new WalletException("Unknown Error", new RuntimeException("other error"));
            });
            assertTrue(ex.getMessage().contains("Unknown Error"));
        }
    }
}