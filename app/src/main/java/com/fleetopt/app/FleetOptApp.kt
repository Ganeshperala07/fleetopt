package com.fleetopt.app

import android.app.Application
import com.fleetopt.app.core.notification.StockoutNotificationManager
import com.fleetopt.app.core.simulation.TelemetrySimulationEngine
import com.fleetopt.app.data.local.FleetOptDatabase
import com.fleetopt.app.data.repository.FleetOptRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class FleetOptApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { FleetOptDatabase.getInstance(this, applicationScope) }
    val repository by lazy { FleetOptRepository(database) }
    val notificationManager by lazy { StockoutNotificationManager(this) }
    val simulationEngine by lazy { TelemetrySimulationEngine(repository, notificationManager, applicationScope) }
    val adminAuthManager by lazy { com.fleetopt.app.core.security.AdminAuthManager.getInstance(this) }
    val themePreferenceManager by lazy { com.fleetopt.app.ui.theme.ThemePreferenceManager.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        notificationManager.createNotificationChannels()
    }
}
