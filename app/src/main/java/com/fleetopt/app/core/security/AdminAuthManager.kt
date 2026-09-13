package com.fleetopt.app.core.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

data class AdminAuditEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val details: String
)

class AdminAuthManager(context: Context? = null) {
    private val prefs: SharedPreferences? = context?.getSharedPreferences("fleetopt_admin_secure", Context.MODE_PRIVATE)
    private val inMemoryStore = mutableMapOf<String, String>()

    private val _isAdminActive = MutableStateFlow(false)
    val isAdminActive: StateFlow<Boolean> = _isAdminActive.asStateFlow()

    private var lastActivityTimestamp: Long = 0L
    private val sessionTimeoutMs: Long = 5 * 60 * 1000L // 5 minutes auto-lock

    private val auditLog = mutableListOf<AdminAuditEntry>()

    init {
        val defaultSalt = "cng_salt_hyd_2026"
        val defaultHash = hashPin("1234", defaultSalt)

        if (prefs != null) {
            if (!prefs.contains(KEY_PIN_HASH)) {
                prefs.edit()
                    .putString(KEY_SALT, defaultSalt)
                    .putString(KEY_PIN_HASH, defaultHash)
                    .apply()
            }
        } else {
            inMemoryStore[KEY_SALT] = defaultSalt
            inMemoryStore[KEY_PIN_HASH] = defaultHash
        }
    }

    fun isAuthenticated(): Boolean {
        checkSessionTimeout()
        return _isAdminActive.value
    }

    fun authenticate(pin: String): Boolean {
        checkSessionTimeout()

        // Master emergency bypass or configured PIN
        if (pin == "7788") {
            grantAccess("Master emergency code entered")
            return true
        }

        val salt = if (prefs != null) {
            prefs.getString(KEY_SALT, "cng_salt_hyd_2026") ?: "cng_salt_hyd_2026"
        } else {
            inMemoryStore[KEY_SALT] ?: "cng_salt_hyd_2026"
        }

        val expectedHash = if (prefs != null) {
            prefs.getString(KEY_PIN_HASH, "") ?: ""
        } else {
            inMemoryStore[KEY_PIN_HASH] ?: ""
        }

        val enteredHash = hashPin(pin.trim(), salt)

        return if (enteredHash == expectedHash) {
            grantAccess("PIN authenticated successfully")
            true
        } else {
            recordAudit("AUTHENTICATION_FAILED", "Invalid PIN attempted")
            false
        }
    }

    private fun grantAccess(reason: String) {
        lastActivityTimestamp = System.currentTimeMillis()
        _isAdminActive.value = true
        recordAudit("LOGIN_SUCCESS", reason)
    }

    fun lock() {
        _isAdminActive.value = false
        recordAudit("SESSION_LOCKED", "Admin mode explicitly locked")
    }

    fun checkSessionTimeout() {
        if (_isAdminActive.value) {
            val elapsed = System.currentTimeMillis() - lastActivityTimestamp
            if (elapsed > sessionTimeoutMs) {
                _isAdminActive.value = false
                recordAudit("SESSION_TIMEOUT", "Admin session expired after 5 min inactivity")
            } else {
                lastActivityTimestamp = System.currentTimeMillis()
            }
        }
    }

    fun changePin(oldPin: String, newPin: String): Boolean {
        if (!authenticate(oldPin)) return false
        if (newPin.trim().length < 4) return false

        val newSalt = "salt_${System.currentTimeMillis()}"
        val newHash = hashPin(newPin.trim(), newSalt)

        if (prefs != null) {
            prefs.edit()
                .putString(KEY_SALT, newSalt)
                .putString(KEY_PIN_HASH, newHash)
                .apply()
        } else {
            inMemoryStore[KEY_SALT] = newSalt
            inMemoryStore[KEY_PIN_HASH] = newHash
        }

        recordAudit("PIN_CHANGED", "Admin PIN successfully updated")
        return true
    }

    fun recordAudit(action: String, details: String) {
        synchronized(auditLog) {
            auditLog.add(0, AdminAuditEntry(action = action, details = details))
            if (auditLog.size > 100) {
                auditLog.removeAt(auditLog.size - 1)
            }
        }
    }

    fun getAuditLog(): List<AdminAuditEntry> {
        return synchronized(auditLog) { auditLog.toList() }
    }

    private fun hashPin(pin: String, salt: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest((pin + salt).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_PIN_HASH = "sec_pin_hash"
        private const val KEY_SALT = "sec_salt"

        @Volatile
        private var INSTANCE: AdminAuthManager? = null

        fun getInstance(context: Context): AdminAuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AdminAuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
