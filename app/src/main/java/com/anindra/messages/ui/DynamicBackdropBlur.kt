package com.anindra.messages.ui

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import top.yukonga.miuix.kmp.blur.effect
import top.yukonga.miuix.kmp.blur.noiseDither
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlurEffect

/** Shared layer ordering for same-frame backdrop composition. */
internal object DynamicBackdropLayer {
    const val BACKGROUND = 0f
    // Settings pre-blurs its static expressive background once; moving cards only copy it.
    const val SETTINGS_BLURRED_BACKGROUND = 0.5f
    const val SETTINGS_CARD = 0.75f
    const val CONTENT = 1f
    const val FLOATING_CONTENT = 2f
    const val HEADER = 3f
    const val NAVIGATION = 4f
    const val SHEET = 5f
    // Sheet children are above the sheet backdrop and must never be fed back into it.
    const val SHEET_CONTENT = 6f
    const val SHEET_FLOATING_CONTENT = 7f
}

/** Full-resolution sheet blur constants, kept separate from the adaptive miuix fallback. */
internal object DynamicBackdropBlurQuality {
    const val NATIVE_EDGE_PADDING_MULTIPLIER = 1.5f

    fun nativeEdgePadding(radiusPx: Float): Float =
        radiusPx.coerceAtLeast(0f) * NATIVE_EDGE_PADDING_MULTIPLIER
}

private data class DynamicBackdropSource(
    val layer: Float,
    val backdrop: LayerBackdrop,
    val order: Long
)

@Stable
private class DynamicBackdropGraph {
    private val movingOwners = mutableStateMapOf<Any, Unit>()
    private val sources = mutableMapOf<Any, DynamicBackdropSource>()
    private val sourceCache = mutableMapOf<Float, Pair<Int, List<LayerBackdrop>>>()
    private val backdropCache = mutableMapOf<Float, Backdrop>()
    private var nextSourceOrder = 0L
    private var sourceRevisionState by mutableIntStateOf(0)

    var motionFrame by mutableIntStateOf(0)
        private set

    val motionActive: Boolean get() = movingOwners.isNotEmpty()
    val sourceRevision: Int get() = sourceRevisionState

    fun setMoving(owner: Any, moving: Boolean) {
        if (moving) movingOwners[owner] = Unit else movingOwners.remove(owner)
    }

    fun advanceMotionFrame() {
        motionFrame = if (motionFrame == Int.MAX_VALUE) 0 else motionFrame + 1
    }

    fun registerSource(owner: Any, layer: Float, backdrop: LayerBackdrop) {
        val current = sources[owner]
        if (current?.layer == layer && current.backdrop === backdrop) return
        nextSourceOrder += 1L
        sources[owner] = DynamicBackdropSource(layer, backdrop, nextSourceOrder)
        invalidateSources()
    }

    fun unregisterSource(owner: Any) {
        if (sources.remove(owner) != null) invalidateSources()
    }

    fun backdropBelow(layer: Float): Backdrop = backdropCache.getOrPut(layer) {
        RegisteredBackdrop(this, layer)
    }

    fun sourcesBelow(layer: Float): List<LayerBackdrop> {
        val revision = sourceRevisionState
        sourceCache[layer]?.takeIf { it.first == revision }?.let { return it.second }
        // Every attachment owns a separate graphics layer. Sorting by attachment order preserves
        // the real screen stack: the always-composed inbox stays below Settings or a chat route,
        // while two-pane sources retain their own non-overlapping window coordinates.
        val selected = sources.values.asSequence()
            .filter { source ->
                when (layer) {
                    DynamicBackdropLayer.SHEET_FLOATING_CONTENT ->
                        source.layer == DynamicBackdropLayer.SHEET_CONTENT
                    DynamicBackdropLayer.SETTINGS_CARD ->
                        source.layer == DynamicBackdropLayer.SETTINGS_BLURRED_BACKGROUND
                    else ->
                        source.layer < layer &&
                            source.layer != DynamicBackdropLayer.SETTINGS_BLURRED_BACKGROUND
                }
            }
            .sortedBy { it.order }
            .map { it.backdrop }
            .toList()
        sourceCache[layer] = revision to selected
        return selected
    }

    private fun invalidateSources() {
        sourceCache.clear()
        sourceRevisionState = if (sourceRevisionState == Int.MAX_VALUE) {
            0
        } else {
            sourceRevisionState + 1
        }
    }
}

/**
 * Draws the currently attached sources into one effect. Adapted from BakaSU Manager's Apache-2.0
 * CombinedBackdrop approach: each owner keeps its own graphics layer and live coordinates, so two
 * simultaneously composed routes can never overwrite one another's sampled texture.
 */
private class RegisteredBackdrop(
    private val graph: DynamicBackdropGraph,
    private val consumerLayer: Float
) : Backdrop {
    override val isCoordinatesDependent: Boolean = true
    override val offsetResidualX: Float
        get() = graph.sourcesBelow(consumerLayer).firstOrNull()?.offsetResidualX ?: 0f
    override val offsetResidualY: Float
        get() = graph.sourcesBelow(consumerLayer).firstOrNull()?.offsetResidualY ?: 0f

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        downscaleFactor: Int
    ) {
        graph.sourcesBelow(consumerLayer).forEach { backdrop ->
            with(backdrop) {
                drawBackdrop(density, coordinates, layerBlock, downscaleFactor)
            }
        }
    }
}

private val LocalDynamicBackdropGraph = staticCompositionLocalOf<DynamicBackdropGraph?> { null }

/** One shared, hardware-accelerated LayerBackdrop graph for all glass surfaces. */
@Composable
internal fun DynamicBackdropHost(content: @Composable () -> Unit) {
    // The dependency's manifest starts at API 33. On Android 10-12, do not initialise any miuix
    // class: translucent surfaces keep their Material tint and the rest of the UI remains intact.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        content()
        return
    }

    val graph = remember { DynamicBackdropGraph() }
    // Scrollers and draggable sheets are commonly translated as cached graphics layers. Refresh
    // only while one is moving so the sampled coordinates follow the current frame, then go idle.
    LaunchedEffect(graph) {
        snapshotFlow { graph.motionActive }.collectLatest { active ->
            if (!active) return@collectLatest
            while (true) {
                withFrameNanos { }
                graph.advanceMotionFrame()
            }
        }
    }
    CompositionLocalProvider(LocalDynamicBackdropGraph provides graph, content = content)
}

/** Keeps live glass aligned while a scroll or sheet offset is changing, then fully suspends. */
@Composable
internal fun DynamicBackdropMotionTracker(
    settleDelayMillis: Long = 80L,
    position: () -> Any?
) {
    val graph = LocalDynamicBackdropGraph.current
    val owner = remember(graph) { Any() }
    val currentPosition by rememberUpdatedState(position)

    LaunchedEffect(graph, owner) {
        if (graph == null) return@LaunchedEffect
        var initial = true
        snapshotFlow { currentPosition() }.collectLatest {
            // Establish the laid-out resting position without waking HWUI. Only later changes are
            // movement; this also avoids a synthetic burst for every tracker during app startup.
            if (initial) {
                initial = false
                return@collectLatest
            }
            // Keep one frame-synchronised refresh loop alive through drags and flings. The graph,
            // not each moving owner, advances the tick so concurrent motions still emit once/frame.
            graph.setMoving(owner, true)
            delay(settleDelayMillis)
            graph.setMoving(owner, false)
        }
    }
    DisposableEffect(graph, owner) {
        onDispose { graph?.setMoving(owner, false) }
    }
}

/** Records this layer once for all higher glass surfaces, without root-view bitmap capture. */
internal fun Modifier.dynamicBackdropSource(
    layer: Float,
    followMotion: Boolean = false
): Modifier = composed {
    // miuix-blur uses Android RuntimeShader, just as BakaSU Manager does.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@composed this
    val graph = LocalDynamicBackdropGraph.current ?: return@composed this
    val owner = remember(graph) { Any() }
    val sheetFallback = MaterialTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop(
        onDraw = {
            if (layer == DynamicBackdropLayer.SHEET_CONTENT) {
                // Only the recorded layer receives this base; the visible sheet keeps its tint.
                drawRect(sheetFallback)
            }
            drawContent()
        }
    )
    DisposableEffect(graph, owner, layer, backdrop) {
        graph.registerSource(owner, layer, backdrop)
        onDispose { graph.unregisterSource(owner) }
    }
    val refreshed = if (followMotion) {
        this.drawWithContent {
            graph.motionFrame
            drawContent()
        }
    } else {
        this
    }
    refreshed.layerBackdrop(backdrop)
}

/** Samples every lower recorded layer with BakaSU Manager's adaptive texture-blur pipeline. */
internal fun Modifier.dynamicBackdropEffect(
    blurRadius: Dp = 25.dp,
    layer: Float = DynamicBackdropLayer.CONTENT,
    followMotion: Boolean = false
): Modifier = composed {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@composed this
    val graph = LocalDynamicBackdropGraph.current ?: return@composed this
    val backdrop = remember(graph, layer) { graph.backdropBelow(layer) }
    drawBackdrop(
        backdrop = backdrop,
        shape = { RectangleShape },
        effects = {
            // Rebuild when a route source attaches or detaches; each source retains independent
            // coordinates instead of racing to overwrite a shared graphics layer.
            graph.sourceRevision
            // Match BakaSU: this state read makes miuix recompute its remembered consumer
            // coordinates on the current frame even when an ancestor only transforms the layer.
            if (followMotion) graph.motionFrame
            if (blurRadius.value > 0f) {
                if (layer == DynamicBackdropLayer.SHEET) {
                    val radiusPx = blurRadius.value * density

                    // Bypass miuix's adaptive low-resolution Gaussian for large sheets. The
                    // backdrop graph still supplies the live pixels, but Android's hardware
                    // RenderEffect now blurs a 1:1 texture instead of enlarging a coarse capture.
                    downscaleFactor = 1
                    padding = maxOf(
                        padding,
                        DynamicBackdropBlurQuality.nativeEdgePadding(radiusPx)
                    )
                    noiseDither(BlurDefaults.NoiseCoefficient)
                    effect(
                        BlurEffect(
                            radiusX = radiusPx,
                            radiusY = radiusPx,
                            edgeTreatment = TileMode.Decal
                        )
                    )
                } else {
                    textureBlurEffect(
                        blurRadiusX = blurRadius.value,
                        // Full-resolution dither hides quantisation bands after texture upscaling.
                        noiseCoefficient = BlurDefaults.NoiseCoefficient
                    )
                }
            }
        }
    )
}

/** A live backdrop layer; callers draw their translucent Material tint above it. */
@Composable
internal fun BoxScope.DynamicBackdropBlur(
    modifier: Modifier,
    blurRadius: Dp = 25.dp,
    layer: Float = DynamicBackdropLayer.CONTENT
) {
    Box(modifier.dynamicBackdropEffect(blurRadius, layer))
}
