package com.wallet.transaction;

import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class TranIsoLevelTest {

    @Test
    void getLevel_matches_JDBC_constants() {
        assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, TranIsoLevel.READ_UNCOMMITTED.getLevel());
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, TranIsoLevel.READ_COMMITTED.getLevel());
        assertEquals(Connection.TRANSACTION_REPEATABLE_READ, TranIsoLevel.REPEATABLE_READ.getLevel());
        assertEquals(Connection.TRANSACTION_SERIALIZABLE, TranIsoLevel.SERIALIZABLE.getLevel());
    }

    @Test
    void getTranIsoLevel_maps_all_and_throws_on_invalid() {
        assertEquals(TranIsoLevel.READ_UNCOMMITTED, TranIsoLevel.getTranIsoLevel(Connection.TRANSACTION_READ_UNCOMMITTED));
        assertEquals(TranIsoLevel.READ_COMMITTED, TranIsoLevel.getTranIsoLevel(Connection.TRANSACTION_READ_COMMITTED));
        assertEquals(TranIsoLevel.REPEATABLE_READ, TranIsoLevel.getTranIsoLevel(Connection.TRANSACTION_REPEATABLE_READ));
        assertEquals(TranIsoLevel.SERIALIZABLE, TranIsoLevel.getTranIsoLevel(Connection.TRANSACTION_SERIALIZABLE));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> TranIsoLevel.getTranIsoLevel(9999));
        assertTrue(ex.getMessage().contains("Invalid transaction isolation level"));
    }
}
