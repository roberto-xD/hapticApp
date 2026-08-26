package com.smartbe.hapticcontrol.presentation.control

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartbe.hapticcontrol.domain.model.HapticCommand

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        TransmitIndicator(
            isBroadcasting = state.isBroadcasting,
            activeMode = state.activeMode
        )

        state.error?.let { message ->
            Spacer(Modifier.height(16.dp))
            ErrorBanner(message = message, onDismiss = viewModel::clearError)
        }

        Spacer(Modifier.height(32.dp))

        SectionLabel("Patrón")
        Spacer(Modifier.height(12.dp))
        ModeRow(
            modes = state.modes,
            activeMode = state.activeMode,
            enabled = state.canInteract,
            onSelect = viewModel::selectMode
        )

        Spacer(Modifier.height(36.dp))

        SectionLabel("Intensidad")
        Spacer(Modifier.height(12.dp))
        IntensityLadder(
            range = state.intensityRange,
            level = state.intensity,
            enabled = state.canInteract,
            onSelect = viewModel::setIntensity
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = viewModel::powerOff,
            enabled = state.canInteract,
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

@Composable
private fun TransmitIndicator(isBroadcasting: Boolean, activeMode: Int?) {
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
                text = activeMode?.let { "Patrón $it activo" } ?: "Sin patrón activo",
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
private fun ModeRow(
    modes: IntRange,
    activeMode: Int?,
    enabled: Boolean,
    onSelect: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        modes.forEach { index ->
            val isActive = index == activeMode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.85f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isActive) Signal else Panel)
                    .border(
                        width = 1.dp,
                        color = if (isActive) Signal else Edge,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .alpha(if (enabled) 1f else 0.45f)
                    .clickable(enabled = enabled) { onSelect(index) }
                    .semantics { contentDescription = "Patrón $index" },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$index",
                    color = if (isActive) Ground else Color(0xFFC3CBD6),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Escalera de peldanos discretos. Cada peldano es un valor real del firmware.
 */
@Composable
private fun IntensityLadder(
    range: IntRange,
    level: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            range.forEach { step ->
                val filled = step <= level
                val height by animateFloatAsState(
                    targetValue = 28f + (step * 3.2f),
                    animationSpec = tween(160),
                    label = "rung$step"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(height.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (filled) Signal else Panel)
                        .alpha(if (enabled) 1f else 0.45f)
                        .clickable(enabled = enabled) { onSelect(step) }
                        .semantics { contentDescription = "Intensidad $step" }
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Nivel $level de ${HapticCommand.MAX_INTENSITY}",
            color = Muted,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )
    }
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
