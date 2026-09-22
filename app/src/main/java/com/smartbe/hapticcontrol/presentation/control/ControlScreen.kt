package com.smartbe.hapticcontrol.presentation.control

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartbe.hapticcontrol.domain.model.HapticMode

/**
 * Panel de control.
 *
 * Dos decisiones de diseno que vienen del hardware, no del gusto:
 *
 * 1. La intensidad se dibuja como escalera de 10 peldanos, no como slider.
 *    El firmware solo acepta 10 valores discretos; un slider continuo mentiria
 *    sobre lo que el aparato puede hacer.
 *
 * 2. El indicador "EMITIENDO" reporta que el anuncio salio del telefono, no que
 *    el dispositivo lo recibio. El enlace es unidireccional y no hay manera de
 *    confirmar la recepcion. La UI no debe fingir certeza que no tiene.
 */

private val Ground = Color(0xFF0F1218)
private val Panel = Color(0xFF181D26)
private val Edge = Color(0xFF2A323F)
private val Signal = Color(0xFFE8A33D)
private val Muted = Color(0xFF6B7683)
private val Halt = Color(0xFFB3453B)

@Composable
fun ControlScreen(
    viewModel: ControlViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) viewModel.toggleAudioReactive()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        TransmitIndicator(
            isBroadcasting = state.isBroadcasting,
            activeName = state.activeMode?.displayName
        )

        state.error?.let { message ->
            Spacer(Modifier.height(16.dp))
            ErrorBanner(message = message, onDismiss = viewModel::clearError)
        }

        Spacer(Modifier.height(32.dp))

        SectionLabel("Continua")
        Spacer(Modifier.height(12.dp))
        state.continuous.forEach { mode ->
            ModeRow(mode, mode == state.activeMode, state.nativeAvailable) {
                viewModel.select(mode)
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(24.dp))

        SectionLabel("Ritmica")
        Spacer(Modifier.height(12.dp))
        state.rhythmic.forEach { mode ->
            ModeRow(mode, mode == state.activeMode, state.nativeAvailable) {
                viewModel.select(mode)
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(24.dp))

        SectionLabel("Reactivo")
        Spacer(Modifier.height(12.dp))
        AudioRow(
            isActive = state.isAudioReactive,
            enabled = state.nativeAvailable,
            onToggle = {
                if (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    viewModel.toggleAudioReactive()
                } else {
                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = viewModel::powerOff,
            enabled = state.nativeAvailable,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isOn) Halt else Panel,
                contentColor = if (state.isOn) Color.White else Muted,
                disabledContainerColor = Panel,
                disabledContentColor = Muted
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Text("Apagar", fontSize = 17.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * El micro solo alcanza las tres continuas: los ritmos tienen tempo propio y no
 * responderian al volumen. Conviene decirselo al usuario, no que lo deduzca.
 */
@Composable
private fun AudioRow(
    isActive: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Panel)
            .border(
                width = 1.dp,
                color = if (isActive) Signal else Edge,
                shape = RoundedCornerShape(12.dp)
            )
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, onClick = onToggle)
            .semantics { contentDescription = "Reactivo al sonido" }
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isActive) Signal else Edge)
        )
        Spacer(Modifier.size(14.dp))
        Column {
            Text(
                text = "Reactivo al sonido",
                color = if (isActive) Signal else Color(0xFFC3CBD6),
                fontSize = 15.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = "Micrófono, tres niveles",
                color = Muted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ModeRow(
    mode: HapticMode,
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Panel)
            .border(
                width = 1.dp,
                color = if (isActive) Signal else Edge,
                shape = RoundedCornerShape(12.dp)
            )
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = mode.displayName }
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isActive) Signal else Edge)
        )
        Spacer(Modifier.size(14.dp))
        Text(
            text = mode.displayName,
            color = if (isActive) Signal else Color(0xFFC3CBD6),
            fontSize = 15.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun TransmitIndicator(isBroadcasting: Boolean, activeName: String?) {
    val dotColor by animateColorAsState(
        targetValue = if (isBroadcasting) Signal else Edge,
        animationSpec = tween(180),
        label = "dot"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.size(10.dp))
        Column {
            Text(
                text = if (isBroadcasting) "Emitiendo" else "En espera",
                color = if (isBroadcasting) Signal else Muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = activeName ?: "Sin programa activo",
                color = Muted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = Muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp
    )
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Panel)
            .border(1.dp, Halt, RoundedCornerShape(10.dp))
            .clickable(onClick = onDismiss)
            .padding(14.dp)
    ) {
        Text(
            text = message,
            color = Color(0xFFE0A9A3),
            fontSize = 13.sp,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
