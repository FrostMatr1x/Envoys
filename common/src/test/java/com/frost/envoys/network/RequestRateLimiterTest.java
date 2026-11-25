package com.frost.envoys.network;

import com.frost.envoys.TestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestRateLimiterTest {

    @BeforeEach
    void setUp() {
        TestSupport.resetRequestRateLimiter();
    }

    @Test
    void allowsTenRequestsThenRejects() {
        UUID player = TestSupport.uuid(1);
        for (int i = 0; i < 10; i++) {
            assertTrue(RequestRateLimiter.tryAcquire(player), "request " + i);
        }
        assertFalse(RequestRateLimiter.tryAcquire(player));
    }

    @Test
    void differentPlayersAreIndependent() {
        UUID first = TestSupport.uuid(1);
        UUID second = TestSupport.uuid(2);
        for (int i = 0; i < 10; i++) {
            assertTrue(RequestRateLimiter.tryAcquire(first));
        }
        assertFalse(RequestRateLimiter.tryAcquire(first));
        assertTrue(RequestRateLimiter.tryAcquire(second));
    }

    @Test
    void nullPlayerIsRejected() {
        assertFalse(RequestRateLimiter.tryAcquire(null));
    }

    @Test
    void resetAllowsRequestsAgain() {
        UUID player = TestSupport.uuid(1);
        for (int i = 0; i < 10; i++) {
            assertTrue(RequestRateLimiter.tryAcquire(player));
        }
        assertFalse(RequestRateLimiter.tryAcquire(player));

        TestSupport.resetRequestRateLimiter();
        assertTrue(RequestRateLimiter.tryAcquire(player));
    }
}
