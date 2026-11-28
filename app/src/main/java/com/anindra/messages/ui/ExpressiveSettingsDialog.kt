package com.anindra.messages.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Shared M3 Expressive glass window for Settings actions.
 *
 * The uneven corners keep the silhouette expressive while retaining the roomy,
 * readable geometry expected from a dialog. Its translucent tint samples the
 * same cached 25 dp Settings backdrop as the cards instead of starting another
 * blur pass for every modal.
 */
@Composable
internal fun ExpressiveSettingsDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val dialogShape = RoundedCornerShape(
        topStart = 40.dp,
        topEnd = 28.dp,
        bottomEnd = 40.dp,
        bottomStart = 28.dp
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            TranslucentBackdropSurface(
                shape = dialogShape,
                tint = colors.surfaceContainerHigh,
                modifier = modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                blurRadius = 0.dp,
                backdropLayer = DynamicBackdropLayer.SETTINGS_CARD,
                followMotion = false,
                blurEnabled = LocalSettingsCardBlurEnabled.current,
                // A modal must remain legible even when Settings cards are configured very clear.
                tintAlpha = LocalSettingsCardAlpha.current.coerceAtLeast(0.52f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    title?.let { titleContent ->
                        CompositionLocalProvider(LocalContentColor provides colors.primary) {
                            ProvideTextStyle(typography.headlineSmall) {
                                titleContent()
                            }
                        }
                    }
                    text?.let { textContent ->
                        Column(
                            Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                        ) {
                            CompositionLocalProvider(LocalContentColor provides colors.onSurfaceVariant) {
                                ProvideTextStyle(typography.bodyMedium) {
                                    textContent()
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dismissButton != null) {
                            dismissButton()
                            Spacer(Modifier.width(8.dp))
                        }
                        CompositionLocalProvider(LocalContentColor provides colors.primary) {
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}
