package com.smartbe.hapticcontrol.presentation.scan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Diálogo para activar/desactivar modo visitante
 */
@Composable
fun VisitorModeDialog(
    onConnectClick: () ->Unit,
    onDisconectClick: () -> Unit,
    onSendClick: () ->Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onConnectClick
                ) {
                    Text("conectar")
                }
                Button(
                    onClick = onSendClick
                ) {
                    Text("enviar")
                }
                Button(
                    onClick = onDisconectClick
                ) {
                    Text("desconectar")
                }
            }
        }
    }
}

