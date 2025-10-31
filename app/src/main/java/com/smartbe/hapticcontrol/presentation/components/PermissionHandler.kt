package com.smartbe.hapticcontrol.presentation.components

import android.Manifest
import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * Gestiona los permisos necesarios para BLE
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler(
    onPermissionsGranted: () -> Unit,
    onPermissionsDenied: () -> Unit
) {
    val context = LocalContext.current

    // Permisos según la versión de Android
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionsState = rememberMultiplePermissionsState(permissions)

    // Observar cambios en el estado de permisos
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            onPermissionsGranted()
        } else {
            onPermissionsDenied()
        }
    }

    // Mostrar diálogo si los permisos fueron denegados
    if (permissionsState.shouldShowRationale) {
        PermissionRationaleDialog(
            onDismiss = { onPermissionsDenied() },
            onConfirm = { permissionsState.launchMultiplePermissionRequest() }
        )
    }

    // Solicitar permisos si no están otorgados
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }
}

/**
 * Diálogo para explicar por qué se necesitan los permisos
 */
@Composable
private fun PermissionRationaleDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permisos necesarios") },
        text = {
            Text(
                "Esta aplicación necesita permisos de Bluetooth y ubicación " +
                        "para escanear y conectarse a dispositivos cercanos."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Conceder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}