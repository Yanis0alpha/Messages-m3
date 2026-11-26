package com.anindra.messages.ui

import android.view.View
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp

/** What the platform says the keyboard covers, with the lying weeded out.
 *
 *  The reported IME inset is a claim, and some OEM builds lie: MIUI with an
 *  enlarged display size hands an edge-to-edge window an inset far larger than
 *  the keyboard, which made the composer's padding reserve nearly the whole
 *  screen — the input row ended up pinned to the top of the window, behind the
 *  header, with the reserved height blank below it, and the conversation went
 *  with it. The window's own visible display frame is the number the OEM
 *  itself places its UI by, so it is trusted over the report and the smaller
 *  of the two wins. A translucent IME does not shrink that frame; in that case
 *  Android's real inset remains the only usable keyboard boundary. */
object ImeGuard {

    /** Debug builds only: set from a launch intent extra, this is read instead
     *  of the platform's reported inset — the standing way to reproduce an
     *  OEM's dishonest report on an emulator whose own report is honest. */
    @Volatile
    var debugReportedBottomPx: Int? = null

    /** The inset to apply: the platform's report, or the debug stand-in. */
    fun reportedBottomPx(platformReportedPx: Int): Int =
        debugReportedBottomPx ?: platformReportedPx

    /** The smaller of the reported inset and what the window can actually
     *  show above the keyboard. A report larger than the visible overlap is
     *  padding that would push content off the window; a report smaller than
     *  it (a device that resized the window for the keyboard) still stands. */
    fun appliedBottomPx(reportedPx: Int, visibleOverlapPx: Int): Int =
        reportedPx.coerceAtMost(visibleOverlapPx.coerceAtLeast(0))

    /** A translucent IME may leave the visible display frame unchanged. In
     *  that case the platform inset is the only real geometry available. The
     *  separate platform value also prevents a debug stand-in from creating a
     *  keyboard inset while Android itself reports no IME. */
    fun appliedBottomPx(
        reportedPx: Int,
        visibleOverlapPx: Int,
        platformReportedPx: Int
    ): Int = if (visibleOverlapPx > 0) {
        appliedBottomPx(reportedPx, visibleOverlapPx)
    } else {
        reportedPx.coerceAtMost(platformReportedPx.coerceAtLeast(0))
    }

    /** How much of the window the keyboard covers, from the window's own
     *  visible display frame: the window's bottom edge on screen against the
     *  frame the system says is visible. */
    fun overlapPx(
        rootTopOnScreenPx: Int,
        rootHeightPx: Int,
        visibleFrameBottomPx: Int
    ): Int = (rootTopOnScreenPx + rootHeightPx - visibleFrameBottomPx).coerceAtLeast(0)

    /** [overlapPx] for a live root view. */
    fun visibleOverlapPx(root: View): Int {
        val frame = android.graphics.Rect()
        root.getWindowVisibleDisplayFrame(frame)
        val position = IntArray(2)
        root.getLocationOnScreen(position)
        return overlapPx(position[1], root.height, frame.bottom)
    }

    /** The padding to apply right now, logged so a dishonest report on a real
     *  phone is visible in logcat without a debugger. */
    fun appliedBottomPx(root: View, reportedPx: Int, platformReportedPx: Int): Int {
        val visible = visibleOverlapPx(root)
        val applied = appliedBottomPx(reportedPx, visible, platformReportedPx)
        android.util.Log.d(
            "ImeGuard",
            "reported=$reportedPx platform=$platformReportedPx " +
                "visible=$visible applied=$applied"
        )
        return applied
    }
}

/** The keyboard's height right now, with an OEM's dishonest report clamped by
 *  [ImeGuard] and a translucent keyboard's real platform inset preserved. */
@Composable
fun imeBottomDp(): Dp {
    val density = LocalDensity.current
    val root = LocalView.current.rootView
    val platformReported = WindowInsets.ime.getBottom(density)
    val reported = ImeGuard.reportedBottomPx(platformReported)
    val applied = androidx.compose.runtime.remember(reported, platformReported) {
        ImeGuard.appliedBottomPx(root, reported, platformReported)
    }
    return with(density) { applied.toDp() }
}

/** The composer's bottom padding: the reported IME inset, clamped to what the
 *  window can actually show above the keyboard. */
@Composable
fun Modifier.imeGuardedPadding(): Modifier = this.then(Modifier.padding(bottom = imeBottomDp()))
