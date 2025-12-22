package com.wallet.service;

import com.wallet.Exceptin.InsufficientBalanceException;
import com.wallet.Exceptin.WalletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    DataSource dataSource;
    @Mock
    Connection connection;
    @Mock
    CallableStatement callableStatement;

    WalletService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new WalletService(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareCall(anyString())).thenReturn(callableStatement);
    }

    @Test
    @DisplayName("transfer success commits and returns requestId")
    void transfer_success() throws Exception {
        String requestId = service.transfer("req-123", 1L, 2L, "USD", 10.0);

        assertEquals("req-123", requestId);
        InOrder inOrder = inOrder(connection, callableStatement);
        inOrder.verify(connection).setAutoCommit(false);
        inOrder.verify(connection).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        inOrder.verify(connection).prepareCall("{call wallet_pkg.transfer(?, ?, ?, ?, ?)}");
        inOrder.verify(callableStatement).setString(1, "req-123");
        inOrder.verify(callableStatement).setLong(2, 1L);
        inOrder.verify(callableStatement).setLong(3, 2L);
        inOrder.verify(callableStatement).setString(4, "USD");
        inOrder.verify(callableStatement).setDouble(5, 10.0);
        inOrder.verify(callableStatement).execute();
        inOrder.verify(connection).commit();

        verify(callableStatement).close();
        verify(connection).close();
    }

    @Test
    @DisplayName("transfer(String...) overload generates requestId and delegates")
    void transfer_overload_generates_uuid_and_delegates() throws Exception {
        // Arrange: Let the service run; we capture the value set into parameter 1
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);

        String returned = service.transfer(1L, 2L, "USD", 5.5);

        assertNotNull(returned);
        assertFalse(returned.isBlank());
        // Verify the generated requestId was passed to JDBC
        verify(callableStatement).setString(eq(1), idCaptor.capture());
        assertEquals(returned, idCaptor.getValue());
    }

    @Nested
    class ErrorMapping {
        private SQLException sql(int code) {
            SQLException e = new SQLException("err", null, code);
            return e;
        }

        @Test
        void maps_20003() throws Exception {
            when(callableStatement.execute()).thenThrow(sql(20003));
            WalletException ex = assertThrows(WalletException.class, () ->
                    service.transfer("id", 1, 2, "USD", 1));
            assertTrue(ex.getMessage().contains("Source and destination accounts must differ"));
            verify(connection).rollback();
        }

        @Test
        void maps_20004_and_closes_resources() throws Exception {
            when(callableStatement.execute()).thenThrow(sql(20004));
            WalletException ex = assertThrows(WalletException.class, () ->
                    service.transfer("id", 1, 2, "USD", 1));
            assertTrue(ex.getMessage().contains("Invalid amount"));
            verify(connection).rollback();
            // try-with-resources should still close resources
            verify(callableStatement).close();
            verify(connection).close();
        }

        @Test
        void maps_20005() throws Exception {
            when(callableStatement.execute()).thenThrow(sql(20005));
            WalletException ex = assertThrows(WalletException.class, () ->
                    service.transfer("id", 1, 2, "USD", 1));
            assertTrue(ex.getMessage().contains("Account not found"));
            verify(connection).rollback();
        }

        @Test
        void maps_20006() throws Exception {
            when(callableStatement.execute()).thenThrow(sql(20006));
            assertThrows(InsufficientBalanceException.class, () ->
                    service.transfer("id", 1, 2, "USD", 1));
            verify(connection).rollback();
        }

        @Test
        void maps_20007_to_20009_and_default() throws Exception {
            int[] codes = {20007, 20008, 20009, 12345};
            for (int code : codes) {
                reset(callableStatement, connection);
                when(dataSource.getConnection()).thenReturn(connection);
                when(connection.prepareCall(anyString())).thenReturn(callableStatement);
                when(callableStatement.execute()).thenThrow(sql(code));
                Class<? extends Throwable> expected = WalletException.class;
                assertThrows(expected, () -> service.transfer("id", 1, 2, "USD", 1));
                verify(connection).rollback();
            }
        }
    }

    @Test
    @DisplayName("connection acquisition failure wraps into WalletException")
    void connection_failure() throws Exception {
        // Clear previous stubs to avoid unnecessary stubbing complaint
        reset(dataSource, connection, callableStatement);

        when(dataSource.getConnection()).thenThrow(new SQLException("down"));
        WalletService local = new WalletService(dataSource);
        WalletException ex = assertThrows(WalletException.class, () ->
                local.transfer("id", 1, 2, "USD", 1));
        assertTrue(ex.getMessage().contains("Database connection error"));
    }
}
