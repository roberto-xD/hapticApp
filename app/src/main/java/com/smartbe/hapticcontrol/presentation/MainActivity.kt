package com.smartbe.hapticcontrol

import android.Manifest
import android.app.AlertDialog
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
import androidx.navigation.compose.rememberNavController
import com.smartbe.hapticcontrol.presentation.navigation.NavGraph
import com.smartbe.hapticcontrol.ui.theme.HapticControlTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val tag = "MainActivity"
    private var permissionsRequested = false

    // ========== LAUNCHERS ==========

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        logPermissionResults(permissions)
        handlePermissionResults(permissions)
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(tag, "Bluetooth enable dialog closed")
    }

    // ========== LIFECYCLE ==========

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(tag, "============================================")
        Log.d(tag, "APP STARTED")
        Log.d(tag, "Android version: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
        Log.d(tag, "SDK: compileSdk=35, targetSdk=35")
        Log.d(tag, "============================================")

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

        // Esperar a que la UI esté lista antes de pedir permisos
        window.decorView.post {
            checkAndRequestAllPermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(tag, "onResume()")
        logCurrentPermissionStatus()

        // Verificar Bluetooth
        checkBluetoothEnabled()
    }

    // ========== PERMISOS ==========

    /**
     * Verifica y solicita TODOS los permisos necesarios
     */
    private fun checkAndRequestAllPermissions() {
        Log.d(tag, "checkAndRequestAllPermissions()")

        if (permissionsRequested) {
            Log.d(tag, "Permissions already requested in this session")
            return
        }

        val permissions = getRequiredPermissions()
        val missing = permissions.filter { !hasPermission(it) }

        if (missing.isEmpty()) {
            Log.d(tag, "✓ All permissions granted")
            onAllPermissionsGranted()
        } else {
            Log.d(tag, "Missing ${missing.size} permission(s):")
            missing.forEach { Log.d(tag, "  - ${it.substringAfterLast('.')}") }

            permissionsRequested = true
            requestPermissionsLauncher.launch(missing.toTypedArray())
        }
    }

    /**
     * Función pública para solicitar permisos desde otras pantallas
     */
    fun checkAndRequestPermissions() {
        Log.d(tag, "Manual permission request from UI")
        permissionsRequested = false // Permitir re-solicitar
        checkAndRequestAllPermissions()
    }

    /**
     * Obtiene lista de permisos según versión de Android
     */
    private fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+)
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            // Android 11 y anteriores
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    /**
     * Verifica si tiene un permiso específico
     */
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    /**
     * Verifica si tiene TODOS los permisos de Bluetooth
     */
    fun hasBluetoothPermissions(): Boolean {
        val permissions = getRequiredPermissions()
        val hasAll = permissions.all { hasPermission(it) }
        return hasAll
    }

    /**
     * Maneja los resultados de la solicitud de permisos
     */
    private fun handlePermissionResults(permissions: Map<String, Boolean>) {
        val allGranted = permissions.values.all { it }

        if (allGranted) {
            Log.d(tag, "✓✓✓ ALL PERMISSIONS GRANTED ✓✓✓")
            onAllPermissionsGranted()
        } else {
            Log.w(tag, "⚠ Some permissions denied")

            val denied = permissions.filterValues { !it }.keys
            val permanentlyDenied = denied.any { permission ->
                !ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
            }

            if (permanentlyDenied) {
                Log.w(tag, "Permissions permanently denied - need settings")
                showPermissionDeniedDialog()
            } else {
                Log.w(tag, "Permissions denied but can request again")
            }
        }
    }

    /**
     * Registra el estado actual de permisos
     */
    private fun logCurrentPermissionStatus() {
        Log.d(tag, "=== CURRENT PERMISSION STATUS ===")
        getRequiredPermissions().forEach { permission ->
            val granted = hasPermission(permission)
            val name = permission.substringAfterLast('.')
            Log.d(tag, "  $name: ${if (granted) "✓ GRANTED" else "✗ DENIED"}")
        }
        Log.d(tag, "================================")
    }

    /**
     * Registra resultados de solicitud de permisos
     */
    private fun logPermissionResults(permissions: Map<String, Boolean>) {
        Log.d(tag, "=== PERMISSION RESULTS ===")
        permissions.forEach { (permission, granted) ->
            val name = permission.substringAfterLast('.')
            Log.d(tag, "  $name: ${if (granted) "✓ GRANTED" else "✗ DENIED"}")
        }
        Log.d(tag, "=========================")
    }

    // ========== BLUETOOTH ==========

    /**
     * Verifica si Bluetooth está habilitado
     */
    private fun checkBluetoothEnabled() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Log.e(tag, "✗ Device doesn't support Bluetooth")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Log.w(tag, "⚠ Bluetooth is disabled")
            requestEnableBluetooth()
        } else {
            Log.d(tag, "✓ Bluetooth is enabled")
        }
    }

    /**
     * Solicita habilitar Bluetooth
     */
    private fun requestEnableBluetooth() {
        Log.d(tag, "Requesting to enable Bluetooth...")
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothLauncher.launch(enableBtIntent)
    }

    // ========== DIÁLOGOS ==========

    /**
     * Muestra diálogo cuando permisos están permanentemente denegados
     */
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permisos Necesarios")
            .setMessage(
                "Esta aplicación necesita permisos de Bluetooth y Ubicación para funcionar.\n\n" +
                        "Los permisos han sido denegados. Por favor, habilítalos manualmente en:\n\n" +
                        "Configuración → Aplicaciones → HapticControl → Permisos"
            )
            .setPositiveButton("Abrir Configuración") { dialog, _ ->
                dialog.dismiss()
                openAppSettings()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Abre la configuración de la aplicación
     */
    fun openAppSettings() {
        Log.d(tag, "Opening app settings...")
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(tag, "Error opening app settings", e)
        }
    }

    // ========== CALLBACKS ==========

    /**
     * Llamado cuando todos los permisos están concedidos
     */
    private fun onAllPermissionsGranted() {
        Log.d(tag, "onAllPermissionsGranted() - App ready to use BLE")
        checkBluetoothEnabled()
    }
}