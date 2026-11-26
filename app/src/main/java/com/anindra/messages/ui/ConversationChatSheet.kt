package com.anindra.messages.ui

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

internal object ChatSheetLayout {
    const val HEIGHT_FRACTION = 0.9f
    const val CONTAINER_ALPHA = 0.88f
    const val SCRIM_ALPHA = 0.18f
    val DRAG_HANDLE_HEIGHT = 48.dp
    val BACKGROUND_BLUR_RADIUS = 32.dp
}

/**
 * Material's modal sheet lives in its own transparent Android window. On
 * Android 12+ the window can therefore blur the inbox underneath the
 * translucent sheet instead of merely painting a milky overlay over it.
 */
@Composable
private fun SheetBackgroundBlur() {
    val view = LocalView.current
    val blurRadiusPx = with(LocalDensity.current) {
        ChatSheetLayout.BACKGROUND_BLUR_RADIUS.roundToPx()
    }
    DisposableEffect(view, blurRadiusPx) {
        val provider = (view as? DialogWindowProvider)
            ?: (view.parent as? DialogWindowProvider)
        val window = provider?.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply {
                blurBehindRadius = blurRadiusPx
            }
            window.setBackgroundBlurRadius(blurRadiusPx)
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && window != null) {
                window.setBackgroundBlurRadius(0)
                window.attributes = window.attributes.apply {
                    blurBehindRadius = 0
                }
                window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationChatSheet(
    onDismissed: () -> Unit,
    onGestureDismiss: () -> Unit,
    content: @Composable (requestDismiss: () -> Unit) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var dismissing by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }

    fun finishDismiss(action: () -> Unit) {
        if (!dismissed) {
            dismissed = true
            action()
        }
    }

    val requestDismiss: () -> Unit = {
        if (!dismissing) {
            dismissing = true
            scope.launch {
                sheetState.hide()
                finishDismiss(onDismissed)
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sheetHeight = maxHeight * ChatSheetLayout.HEIGHT_FRACTION
        // Material's official handle occupies 48 dp including its vertical
        // padding. Subtract it so the complete sheet remains exactly 90% high.
        val sheetContentHeight = sheetHeight - ChatSheetLayout.DRAG_HANDLE_HEIGHT
        val windowHeightPx = with(LocalDensity.current) { maxHeight.roundToPx() }
        ModalBottomSheet(
            onDismissRequest = { finishDismiss(onGestureDismiss) },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            containerColor = MaterialTheme.colorScheme.surface.copy(
                alpha = ChatSheetLayout.CONTAINER_ALPHA
            ),
            scrimColor = MaterialTheme.colorScheme.scrim.copy(
                alpha = ChatSheetLayout.SCRIM_ALPHA
            ),
            tonalElevation = 6.dp,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            // The sheet's Android window uses adjustNothing, so Material 3
            // must reserve the IME itself. IME is zero while closed, keeping
            // the sheet flush with the screen without adding a permanent gap.
            contentWindowInsets = { WindowInsets.ime }
        ) {
            SheetBackgroundBlur()
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(sheetContentHeight)
                    .onGloballyPositioned { coordinates ->
                        val top = coordinates.positionInWindow().y.roundToInt()
                        val bottom = top + coordinates.size.height
                        android.util.Log.d(
                            "ChatSheetBounds",
                            "top=$top bottom=$bottom bottomGap=${windowHeightPx - bottom}"
                        )
                    }
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    propagateMinConstraints = true
                ) {
                    content(requestDismiss)
                }
            }
        }
    }
}
