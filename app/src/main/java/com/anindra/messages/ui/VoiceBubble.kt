package com.anindra.messages.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anindra.messages.R
import com.anindra.messages.ui.theme.LocalReduceMotion
import com.anindra.messages.ui.theme.incomingBubble
import com.anindra.messages.ui.theme.motionSpring
import com.anindra.messages.ui.theme.motionTween
import com.anindra.messages.ui.theme.onSelectedBubble
import com.anindra.messages.ui.theme.outgoingBubble
import com.anindra.messages.ui.theme.selectedBubble
import com.anindra.messages.voice.VoiceNotePolicy
import com.anindra.messages.voice.VoicePlayback

internal object VoiceBubbleLayout {
    const val ARCH_LEFT_ROTATION_DEGREES = -90f
    val MIN_WIDTH = 280.dp
    val MAX_WIDTH = 300.dp
    val THIN_PROGRESS_STROKE = 4.dp
    val THIN_PROGRESS_HEIGHT = 10.dp
    val PROGRESS_GAP = 4.dp
    val STOP_INDICATOR_SIZE = 4.dp
    val BASELINE_WAVELENGTH = 40.dp
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private class ExpressiveVoiceControlShape(
    private val morph: Morph,
    private val progress: Float,
    private val rotationDegrees: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = morph.toPath(progress.coerceIn(0f, 1f))
        path.transform(Matrix().apply { scale(x = size.width, y = size.height) })
        path.translate(size.center - path.getBounds().center)
        path.transform(
            Matrix().apply {
                translate(size.center.x, size.center.y)
                rotateZ(rotationDegrees)
                translate(-size.center.x, -size.center.y)
            }
        )
        return Outline.Generic(path)
    }
}

/**
 * A voice note in the conversation, laid out the way Google Messages draws one:
 * an Arch-to-Cookie play control, an expressive wavy playback indicator, and
 * the duration in its own trailing pill.
 *
 * The length is measured rather than assumed because an MMS voice note carries
 * no duration column. Playback progress drives the Material 3 expressive wave.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun VoiceBubble(
    uri: String,
    isMe: Boolean,
    isSelected: Boolean = false,
    body: AnnotatedString = AnnotatedString(""),
    bodyStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    onLongPress: () -> Unit = {}
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val state by VoicePlayback.state.collectAsStateWithLifecycle()
    val mine = state.uri == uri
    val playing = mine && state.playing
    val reduceMotion = LocalReduceMotion.current
    var durationMs by remember(uri) { mutableIntStateOf(VoicePlayback.cachedDuration(uri)) }
    LaunchedEffect(uri) {
        if (durationMs <= 0) durationMs = VoicePlayback.measureDuration(context, uri)
    }
    val total = if (mine && state.durationMs > 0) state.durationMs else durationMs
    val position = if (mine && (playing || state.positionMs > 0)) state.positionMs else 0
    val progress = if (total > 0) (position.toFloat() / total).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = motionTween(reduceMotion, 220),
        label = "voiceBubbleProgress"
    )
    val controlMorph = remember {
        Morph(MaterialShapes.Arch, MaterialShapes.Cookie6Sided)
    }
    val controlMorphProgress by animateFloatAsState(
        targetValue = if (playing) 1f else 0f,
        animationSpec = motionSpring(reduceMotion),
        label = "voiceBubbleControlMorph"
    )
    val controlShape = remember(controlMorph, controlMorphProgress) {
        ExpressiveVoiceControlShape(
            morph = controlMorph,
            progress = controlMorphProgress,
            rotationDegrees = VoiceBubbleLayout.ARCH_LEFT_ROTATION_DEGREES
        )
    }
    val strokeWidth = with(LocalDensity.current) {
        VoiceBubbleLayout.THIN_PROGRESS_STROKE.toPx()
    }
    val progressStroke = remember(strokeWidth) {
        Stroke(width = strokeWidth, cap = StrokeCap.Round)
    }
    val ink = if (isSelected) cs.onSelectedBubble
    else if (isMe) cs.onPrimaryContainer else cs.onSurface
    val accent = if (isSelected) cs.onSelectedBubble
    else if (isMe) cs.onPrimaryContainer else cs.primary
    val lengthText = if (position > 0) {
        "${VoiceNotePolicy.label(position.toLong())} / ${VoiceNotePolicy.label(total.toLong())}"
    } else {
        VoiceNotePolicy.label(total.toLong())
    }

    Surface(
        color = if (isSelected) cs.selectedBubble else if (isMe) cs.outgoingBubble else cs.incomingBubble,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            // Outgoing bubbles stay right-aligned, so the extra width grows
            // toward the left without moving their trailing edge.
            .widthIn(
                min = VoiceBubbleLayout.MIN_WIDTH,
                max = VoiceBubbleLayout.MAX_WIDTH
            )
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
    ) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = { VoicePlayback.toggle(context, uri) },
                    modifier = Modifier.size(48.dp),
                    shape = controlShape,
                    color = accent.copy(alpha = 0.14f),
                    contentColor = accent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(
                                if (playing) {
                                    R.string.access_voice_pause
                                } else {
                                    R.string.access_voice_play
                                }
                            ),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                LinearWavyProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(VoiceBubbleLayout.THIN_PROGRESS_HEIGHT)
                        .padding(horizontal = 8.dp),
                    color = accent,
                    trackColor = accent.copy(alpha = 0.24f),
                    stroke = progressStroke,
                    trackStroke = progressStroke,
                    gapSize = VoiceBubbleLayout.PROGRESS_GAP,
                    stopSize = VoiceBubbleLayout.STOP_INDICATOR_SIZE,
                    amplitude = { if (reduceMotion) 0f else 1f },
                    wavelength = VoiceBubbleLayout.BASELINE_WAVELENGTH
                )
                Surface(
                    shape = CircleShape,
                    color = ink.copy(alpha = 0.12f),
                    contentColor = ink
                ) {
                    Text(
                        text = lengthText,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            if (body.text.isNotBlank()) {
                Text(
                    text = body,
                    style = bodyStyle,
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 2.dp, bottom = 4.dp)
                )
            }
        }
    }
}
