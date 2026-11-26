package com.anindra.messages.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/** Prevents a same-window backdrop capture from drawing the glass over itself. */
internal class BackdropCaptureGuard {
    var active: Boolean = false
}

private class DynamicBackdropBlurView(
    context: Context,
    private val backgroundRoot: View,
    private val captureGuard: BackdropCaptureGuard?,
    blurRadiusPx: Float
) : View(context) {
    private var sampledX = Float.NaN
    private var sampledY = Float.NaN
    private val rootLocation = IntArray(2)
    private val ownLocation = IntArray(2)

    init {
        setWillNotDraw(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setRenderEffect(
                RenderEffect.createBlurEffect(
                    blurRadiusPx,
                    blurRadiusPx,
                    Shader.TileMode.CLAMP
                )
            )
        }
    }

    fun follow(position: Offset) {
        if (sampledX != position.x || sampledY != position.y) {
            sampledX = position.x
            sampledY = position.y
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            !backgroundRoot.isAttachedToWindow ||
            captureGuard?.active == true
        ) {
            return
        }
        backgroundRoot.getLocationOnScreen(rootLocation)
        getLocationOnScreen(ownLocation)
        val checkpoint = canvas.save()
        captureGuard?.active = true
        try {
            canvas.translate(
                (rootLocation[0] - ownLocation[0]).toFloat(),
                (rootLocation[1] - ownLocation[1]).toFloat()
            )
            backgroundRoot.draw(canvas)
        } finally {
            captureGuard?.active = false
            canvas.restoreToCount(checkpoint)
        }
    }
}

/**
 * A true Android 12+ backdrop blur clipped to the bounds supplied by its
 * [BoxScope]. Updating [samplePosition] redraws the sampled pixels while a
 * sheet or floating control moves. Older Android versions keep the translucent
 * Material tint rendered above this layer.
 */
@Composable
internal fun BoxScope.DynamicBackdropBlur(
    backgroundRoot: View,
    samplePosition: Offset,
    modifier: Modifier,
    captureGuard: BackdropCaptureGuard? = null,
    blurRadius: Dp = 32.dp
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val blurRadiusPx = with(LocalDensity.current) { blurRadius.toPx() }
    AndroidView(
        factory = { context ->
            DynamicBackdropBlurView(
                context = context,
                backgroundRoot = backgroundRoot,
                captureGuard = captureGuard,
                blurRadiusPx = blurRadiusPx
            )
        },
        update = { blurView -> blurView.follow(samplePosition) },
        modifier = modifier
    )
}
