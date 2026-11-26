package com.anindra.messages.ui

import android.content.Context
import android.graphics.Bitmap
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
    private var snapshot: Bitmap? = null
    private var capturePosted = false
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

    private fun sharesWindowWithBackdrop(): Boolean =
        isAttachedToWindow && backgroundRoot.rootView === rootView

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scheduleSameWindowCapture()
    }

    override fun onDetachedFromWindow() {
        snapshot?.recycle()
        snapshot = null
        capturePosted = false
        super.onDetachedFromWindow()
    }

    fun follow(position: Offset) {
        val moved = sampledX != position.x || sampledY != position.y
        sampledX = position.x
        sampledY = position.y
        if (sharesWindowWithBackdrop()) {
            // AndroidView's update also runs when the inbox under a stationary
            // navigation bar recomposes, so its snapshot stays current.
            scheduleSameWindowCapture()
        } else if (moved) {
            invalidate()
        }
    }

    /**
     * Redrawing a hardware-accelerated root from one of its own children while
     * its RenderNode is recording crashes with "Recording currently in
     * progress". Capture that same-window root later into a software bitmap;
     * the view's RenderEffect blurs the safe snapshot on its next real draw.
     */
    private fun scheduleSameWindowCapture() {
        if (!sharesWindowWithBackdrop() || capturePosted || width <= 0 || height <= 0) return
        capturePosted = true
        post {
            capturePosted = false
            if (!sharesWindowWithBackdrop() || width <= 0 || height <= 0) return@post

            val next = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val captureCanvas = Canvas(next)
            backgroundRoot.getLocationOnScreen(rootLocation)
            getLocationOnScreen(ownLocation)
            captureCanvas.translate(
                (rootLocation[0] - ownLocation[0]).toFloat(),
                (rootLocation[1] - ownLocation[1]).toFloat()
            )

            captureGuard?.active = true
            val captured = try {
                backgroundRoot.draw(captureCanvas)
                true
            } catch (_: IllegalStateException) {
                false
            } finally {
                captureGuard?.active = false
            }

            if (captured) {
                // Do not explicitly recycle the previous snapshot: the render
                // thread may still reference its display-list texture until
                // this invalidation is committed. GC releases it safely later.
                snapshot = next
                invalidate()
            } else {
                next.recycle()
            }
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
        if (sharesWindowWithBackdrop()) {
            snapshot?.let { canvas.drawBitmap(it, 0f, 0f, null) }
            if (snapshot == null) scheduleSameWindowCapture()
            return
        }

        backgroundRoot.getLocationOnScreen(rootLocation)
        getLocationOnScreen(ownLocation)
        val checkpoint = canvas.save()
        try {
            canvas.translate(
                (rootLocation[0] - ownLocation[0]).toFloat(),
                (rootLocation[1] - ownLocation[1]).toFloat()
            )
            backgroundRoot.draw(canvas)
        } finally {
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
