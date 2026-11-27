package com.anindra.messages.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anindra.messages.R
import com.anindra.messages.ui.theme.LocalReduceMotion
import java.text.BreakIterator
import kotlinx.coroutines.delay

internal object TopLevelHeaderDockLayout {
    const val TINT_ALPHA = 0.30f
    const val ERASE_CHARACTER_DELAY_MS = 14L
    const val TYPE_CHARACTER_DELAY_MS = 22L
    val HEIGHT = 168.dp
    val BOTTOM_CORNER = 32.dp
    val TOP_CONTROL_HEIGHT = 64.dp
    val SEARCH_HEIGHT = 56.dp
}

internal fun typewriterFrames(text: String): List<String> {
    if (text.isEmpty()) return listOf("")
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(text)
    val frames = mutableListOf("")
    iterator.first()
    var boundary = iterator.next()
    while (boundary != BreakIterator.DONE) {
        frames += text.substring(0, boundary)
        boundary = iterator.next()
    }
    return frames
}

@Composable
internal fun TypewriterSwapText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null
) {
    val reduceMotion = LocalReduceMotion.current
    var displayedText by remember { mutableStateOf(if (reduceMotion) text else "") }
    LaunchedEffect(text, reduceMotion) {
        if (reduceMotion) {
            displayedText = text
            return@LaunchedEffect
        }
        typewriterFrames(displayedText)
            .dropLast(1)
            .asReversed()
            .forEach { frame ->
                displayedText = frame
                delay(TopLevelHeaderDockLayout.ERASE_CHARACTER_DELAY_MS)
            }
        typewriterFrames(text).drop(1).forEach { frame ->
            displayedText = frame
            delay(TopLevelHeaderDockLayout.TYPE_CHARACTER_DELAY_MS)
        }
    }
    Text(
        text = displayedText,
        modifier = modifier,
        style = style,
        fontWeight = fontWeight,
        color = color,
        textAlign = textAlign,
        maxLines = 1
    )
}

/**
 * The edge-attached glass dock shared by Messages, Archives and Settings.
 * Its background reaches the top and both side edges, while only the two
 * bottom corners round away like the exposed edge of a sheet.
 */
@Composable
internal fun TopLevelHeaderDock(
    title: String,
    modifier: Modifier = Modifier,
    topContent: @Composable BoxScope.() -> Unit
) {
    val dockShape = RoundedCornerShape(
        bottomStart = TopLevelHeaderDockLayout.BOTTOM_CORNER,
        bottomEnd = TopLevelHeaderDockLayout.BOTTOM_CORNER
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(TopLevelHeaderDockLayout.HEIGHT),
        shape = dockShape,
        color = Color.Transparent,
        shadowElevation = 8.dp
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(dockShape)
                .dynamicBackdropSource(DynamicBackdropLayer.HEADER)
                .dynamicBackdropEffect(
                    25.dp,
                    DynamicBackdropLayer.HEADER,
                    followMotion = true
                )
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                            alpha = TopLevelHeaderDockLayout.TINT_ALPHA
                        )
                    )
            )
            Column(
                // Only the controls avoid the status bar. The glass surface itself
                // stays attached to the physical top edge and grows downward.
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TopLevelHeaderDockLayout.TOP_CONTROL_HEIGHT),
                    contentAlignment = Alignment.Center,
                    content = topContent
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    TypewriterSwapText(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** The always-visible search control placed at the top of [TopLevelHeaderDock]. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TopLevelDockSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onFocusChanged: (Boolean) -> Unit = {},
    onClear: () -> Unit = { onQueryChange("") }
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { TypewriterSwapText(text = placeholder) },
        singleLine = true,
        leadingIcon = {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialShapes.Cookie6Sided.toShape(),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Search,
                        stringResource(R.string.icon_search),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = onClear) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.icon_close_search))
                }
            }
        } else {
            null
        },
        shape = RoundedCornerShape(TopLevelHeaderDockLayout.SEARCH_HEIGHT / 2f),
        colors = TextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                alpha = 0.78f
            ),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                alpha = 0.68f
            ),
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(TopLevelHeaderDockLayout.SEARCH_HEIGHT)
            .onFocusChanged { onFocusChanged(it.isFocused) }
    )
}
