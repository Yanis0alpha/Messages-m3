package com.anindra.messages.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.anindra.messages.R
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

internal object ChatSheetLayout {
    const val HEIGHT_FRACTION = 0.9f
    val DRAG_HANDLE_HEIGHT = 40.dp
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
    val closeLabel = stringResource(R.string.common_close)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sheetHeight = maxHeight * ChatSheetLayout.HEIGHT_FRACTION
        val windowHeightPx = with(LocalDensity.current) { maxHeight.roundToPx() }
        ModalBottomSheet(
            onDismissRequest = { finishDismiss(onGestureDismiss) },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            containerColor = MaterialTheme.colorScheme.background,
            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
            tonalElevation = 6.dp,
            dragHandle = null,
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(sheetHeight)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ChatSheetLayout.DRAG_HANDLE_HEIGHT)
                        .semantics {
                            contentDescription = closeLabel
                            onClick {
                                requestDismiss()
                                true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    BottomSheetDefaults.DragHandle()
                }
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
