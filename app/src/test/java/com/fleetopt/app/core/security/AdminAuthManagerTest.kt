package com.fleetopt.app.core.security

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AdminAuthManagerTest {

    private lateinit var authManager: AdminAuthManager

    @Before
    fun setUp() {
        authManager = AdminAuthManager()
    }

    @Test
    fun testDefaultPinAuthentication() {
        assertFalse("Initially should not be authenticated", authManager.isAuthenticated())

        val success = authManager.authenticate("1234")
        assertTrue("Default PIN 1234 should authenticate successfully", success)
        assertTrue("State should now be authenticated", authManager.isAuthenticated())
    }

    @Test
    fun testMasterPinAuthentication() {
        val success = authManager.authenticate("7788")
        assertTrue("Master recovery PIN 7788 should authenticate successfully", success)
        assertTrue(authManager.isAuthenticated())
    }

    @Test
    fun testInvalidPinAuthentication() {
        val success = authManager.authenticate("9999")
        assertFalse("Invalid PIN should fail", success)
        assertFalse("State should remain unauthenticated", authManager.isAuthenticated())
    }

    @Test
    fun testNoLockoutOnConsecutiveFailures() {
        // User explicitly requested: NO 3-strike rate limitation lockout
        for (i in 1..10) {
            val success = authManager.authenticate("wrong-$i")
            assertFalse(success)
        }

        // Subsequent valid attempt must succeed immediately without any lockout
        val validSuccess = authManager.authenticate("1234")
        assertTrue("Valid PIN must succeed after any number of failures (no lockout)", validSuccess)
        assertTrue(authManager.isAuthenticated())
    }

    @Test
    fun testLockAdmin() {
        authManager.authenticate("1234")
        assertTrue(authManager.isAuthenticated())

        authManager.lock()
        assertFalse("After lock(), state must be unauthenticated", authManager.isAuthenticated())
    }

    @Test
    fun testChangePinSuccess() {
        val changeSuccess = authManager.changePin("1234", "5678")
        assertTrue("Should successfully change PIN with valid old PIN", changeSuccess)

        // Old PIN should now fail
        val oldPinAuth = authManager.authenticate("1234")
        assertFalse("Old PIN should fail after change", oldPinAuth)

        // New PIN should succeed
        val newPinAuth = authManager.authenticate("5678")
        assertTrue("New PIN should succeed", newPinAuth)
    }

    @Test
    fun testChangePinFailureWithIncorrectOldPin() {
        val changeFail = authManager.changePin("0000", "5678")
        assertFalse("Changing PIN with wrong old PIN must fail", changeFail)

        // Original PIN should still work
        val authOriginal = authManager.authenticate("1234")
        assertTrue("Original PIN must remain valid", authOriginal)
    }
}
