package com.example.data

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.api.EnterpriseBackend
import com.example.api.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * SQLiteSyncService is an Android Service that monitors network status in the background
 * and automatically triggers bidirectional SQLite synchronization when network connectivity
 * is restored. This ensures student progress and learning materials are perfectly aligned
 * without requiring manual user intervention.
 */
class SQLiteSyncService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var connectivityManager: ConnectivityManager? = null
    private val isSyncing = AtomicBoolean(false)
    private var wasOffline = false

    private val _serviceStatus = MutableStateFlow("Initialized")
    val serviceStatus: StateFlow<String> = _serviceStatus.asStateFlow()

    inner class LocalBinder : Binder() {
        fun getService(): SQLiteSyncService = this@SQLiteSyncService
    }

    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SQLiteSyncService onCreate")
        _serviceStatus.value = "Running (Monitoring Network)"

        EnterpriseBackend.writeLog(
            LogLevel.INFO,
            "SYNC_SERVICE",
            "SQLiteSyncService started successfully. Monitoring dynamic network interfaces."
        )

        setupNetworkCallback()
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d(TAG, "Network is AVAILABLE")
            
            _serviceStatus.value = "Online (Ready)"
            
            // Trigger automatic SQLite sync if connection was restored
            if (wasOffline) {
                EnterpriseBackend.writeLog(
                    LogLevel.INFO,
                    "SYNC_SERVICE",
                    "Network connection RESTORED. Triggering automatic background SQLite upload/download sync..."
                )
                triggerAutoSync()
                wasOffline = false
            } else {
                // First connection callback when starting
                EnterpriseBackend.writeLog(
                    LogLevel.INFO,
                    "SYNC_SERVICE",
                    "Network is active. SQLite baseline database aligned."
                )
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Log.d(TAG, "Network is LOST")
            wasOffline = true
            _serviceStatus.value = "Offline (Local Queue Enabled)"
            
            EnterpriseBackend.writeLog(
                LogLevel.WARNING,
                "SYNC_SERVICE",
                "Network connection lost. Diverting updates to local SQLite database tables."
            )
        }
    }

    private fun setupNetworkCallback() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager != null) {
            val builder = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            try {
                connectivityManager?.registerNetworkCallback(builder.build(), networkCallback)
                
                // Query initial status
                val activeNetwork = connectivityManager?.activeNetwork
                val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
                val isOnline = capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )
                wasOffline = !isOnline
                _serviceStatus.value = if (isOnline) "Online (Ready)" else "Offline (Local Queue Enabled)"
            } catch (e: Exception) {
                Log.e(TAG, "Failed registering network callback", e)
                EnterpriseBackend.writeLog(
                    LogLevel.ERROR,
                    "SYNC_SERVICE",
                    "Failed to register network callback: ${e.message}"
                )
            }
        }
    }

    fun triggerAutoSync() {
        if (isSyncing.compareAndSet(false, true)) {
            _serviceStatus.value = "Synchronizing SQLite Database..."
            serviceScope.launch {
                try {
                    EnterpriseBackend.writeLog(
                        LogLevel.INFO,
                        "SYNC_SERVICE",
                        "Starting automatic upload of student progress and learning materials to central server..."
                    )
                    
                    val strategy = EnterpriseBackend.syncStrategy.value
                    val resolvedConflicts = SQLiteSyncAdapter.synchronize(applicationContext, strategy)
                    
                    EnterpriseBackend.writeLog(
                        LogLevel.INFO,
                        "SYNC_SERVICE",
                        "Auto-sync completed successfully. Resolved ${resolvedConflicts.size} conflicts using strategy: ${strategy.name}"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in background sync service", e)
                    EnterpriseBackend.writeLog(
                        LogLevel.ERROR,
                        "SYNC_SERVICE",
                        "Background automatic synchronization failed: ${e.message}"
                    )
                } finally {
                    isSyncing.set(false)
                    _serviceStatus.value = "Online (Ready)"
                }
            }
        } else {
            Log.d(TAG, "AutoSync already in progress, skipping")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "SQLiteSyncService onStartCommand")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "SQLiteSyncService onBind")
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "SQLiteSyncService onDestroy")
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering callback", e)
        }
        serviceScope.cancel()
        EnterpriseBackend.writeLog(
            LogLevel.WARNING,
            "SYNC_SERVICE",
            "SQLiteSyncService stopped. Background automatic sync monitoring deactivated."
        )
    }

    companion object {
        private const val TAG = "SQLiteSyncService"
    }
}
