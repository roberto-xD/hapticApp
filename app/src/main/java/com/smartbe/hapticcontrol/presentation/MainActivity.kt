package com.smartbe.hapticcontrol

import android.Manifest
import android.R.attr.tag
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.compose.rememberNavController
import com.google.android.datatransport.runtime.scheduling.persistence.EventStoreModule_PackageNameFactory.packageName
import com.smartbe.hapticcontrol.presentation.navigation.NavGraph
import com.smartbe.hapticcontrol.ui.theme.HapticControlTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val tag = "MainActivity"

    // ========== PERMISOS SEGÚN VERSIÓN DE ANDROID ==========

    private val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+ (API 31+)
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.ACCESS_FINE_LOCATION // CRÍTICO para BLE scan
        )
    } else {
        // Android 11 y anteriores
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION, // CRÍTICO para BLE scan
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    // ========== LAUNCHERS ==========

    /**
     * Launcher para solicitar múltiples permisos
     */
    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d(tag, "=== Permissions Result ===")
        permissions.entries.forEach { (permission, granted) ->
            Log.d(tag, "  ${permission.substringAfterLast('.')}: ${if (granted) "✓ GRANTED" else "✗ DENIED"}")
        }
        Log.d(tag, "=========================")

        val allGranted = permissions.entries.all { it.value }

        if (allGranted) {
            Log.d(tag, "✓ All permissions granted!")
            onPermissionsGranted()
        } else {
            Log.w(tag, "✗ Some permissions denied")

            // Verificar si algún permiso fue denegado permanentemente
            val deniedPermissions = permissions.filterValues { !it }.keys
            val shouldShowRationale = deniedPermissions.any { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
            }

            if (!shouldShowRationale) {
                // Denegado permanentemente - necesita ir a configuración
                Log.w(tag, "Permissions permanently denied - need to go to settings")
            }

            onPermissionsDenied(permissions)
        }
    }

    /**
     * Launcher para habilitar Bluetooth
     */
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d(tag, "✓ Bluetooth enabled by user")
            checkAndRequestPermissions()
        } else {
            Log.w(tag, "✗ User declined to enable Bluetooth")
        }
    }

    // ========== LIFECYCLE ==========

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(tag, "========================================")
        Log.d(tag, "onCreate called")
        Log.d(tag, "Android version: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
        Log.d(tag, "========================================")

        // Verificar estado inicial
        logPermissionStatus()
        checkBluetoothEnabled()

        setContent {
            HapticControlTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(tag, "onResume called")
        logPermissionStatus()
    }

    // ========== FUNCIONES DE PERMISOS ==========

    /**
     * Log detallado del estado de permisos
     */
    private fun logPermissionStatus() {
        Log.d(tag, "=== Current Permission Status ===")
        bluetoothPermissions.forEach { permission ->
            val granted = ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED
            val permissionName = permission.substringAfterLast('.')
            val status = if (granted) "✓ GRANTED" else "✗ DENIED"
            Log.d(tag, "  $permissionName: $status")
        }
        Log.d(tag, "=================================")
    }

    /**
     * Verifica si Bluetooth está habilitado
     */
    private fun checkBluetoothEnabled() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Log.e(tag, "✗ Device does not support Bluetooth")
            return
        }

        val isEnabled = bluetoothAdapter.isEnabled
        Log.d(tag, "Bluetooth enabled: ${if (isEnabled) "✓ YES" else "✗ NO"}")

        if (!isEnabled) {
            Log.w(tag, "Requesting user to enable Bluetooth...")
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        }
    }

    /**
     * Verifica y solicita permisos faltantes
     * FUNCIÓN PÚBLICA para que ScanScreen pueda llamarla
     */
    fun checkAndRequestPermissions() {
        Log.d(tag, "checkAndRequestPermissions() called")

        val permissionsToRequest = mutableListOf<String>()

        // Verificar cada permiso
        bluetoothPermissions.forEach { permission ->
            val granted = ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED

            if (!granted) {
                permissionsToRequest.add(permission)
                Log.d(tag, "  Need to request: ${permission.substringAfterLast('.')}")
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            Log.d(tag, "Requesting ${permissionsToRequest.size} permission(s)...")

            // Mostrar explicación antes de solicitar permisos críticos
            if (permissionsToRequest.contains(Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.d(tag, "ℹ Location permission required for BLE scanning")
            }

            requestMultiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Log.d(tag, "✓ All permissions already granted")
            onPermissionsGranted()
        }
    }

    /**
     * Verifica si todos los permisos están concedidos
     */
    fun hasBluetoothPermissions(): Boolean {
        val hasAll = bluetoothPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED
        }
        Log.d(tag, "hasBluetoothPermissions: ${if (hasAll) "✓ YES" else "✗ NO"}")
        return hasAll
    }

    /**
     * Verifica si se debe mostrar explicación para un permiso
     */
    private fun shouldShowRationale(permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
    }

    /**
     * Abre la configuración de la app para permisos
     */
    fun openAppSettings() {
        Log.d(tag, "Opening app settings...")
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    // ========== CALLBACKS ==========

    /**
     * Llamado cuando todos los permisos fueron concedidos
     */
    private fun onPermissionsGranted() {
        Log.d(tag, "✓✓✓ All permissions granted - App ready to use BLE ✓✓✓")
    }

    /**
     * Llamado cuando algunos permisos fueron denegados
     */
    private fun onPermissionsDenied(permissions: Map<String, Boolean>) {
        Log.w(tag, "⚠ Some permissions were denied")

        val deniedPermissions = permissions.filterValues { !it }.keys

        deniedPermissions.forEach { permission ->
            Log.w(tag, "  Denied: ${permission.substringAfterLast('.')}")

            if (!shouldShowRationale(permission)) {
                Log.w(tag, "    → Permanently denied (user must enable in settings)")
            } else {
                Log.w(tag, "    → Can request again")
            }
        }
    }
}