package com.anindra.messages.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingChatChromeTest {
    private val root: File by lazy {
        generateSequence(File("").absoluteFile) { it.parentFile }
            .first { File(it, "app/src/main").isDirectory }
    }

    private val chat: String by lazy {
        File(root, "app/src/main/java/com/anindra/messages/ui/ChatScreen.kt").readText()
    }

    private val activity: String by lazy {
        File(root, "app/src/main/java/com/anindra/messages/MainActivity.kt").readText()
    }

    private val previews: String by lazy {
        File(root, "app/src/debug/java/com/anindra/messages/ui/previews/ChatPreviews.kt").readText()
    }

    private val versions: String by lazy {
        File(root, "gradle/libs.versions.toml").readText()
    }

    private val appBuild: String by lazy {
        File(root, "app/build.gradle.kts").readText()
    }

    private val mmsBuild: String by lazy {
        File(root, "mms/build.gradle.kts").readText()
    }

    @Test
    fun discussionHeaderUsesARoundedContainerAndLeftParticipantPill() {
        val overlay = chat.substringAfter("internal fun FloatingChatHeader(")
            .substringBefore("internal fun MessageSelectionToolbar(")
        val toolbar = chat.substringAfter("internal fun ChatTopBar(")
            .substringBefore("internal fun ChatMessageList(")

        assertTrue(overlay.contains("Surface("))
        assertTrue(overlay.contains("shape = RoundedCornerShape(28.dp)"))
        assertTrue(overlay.contains("color = MaterialTheme.colorScheme.surfaceContainer"))
        assertTrue(overlay.contains("shadowElevation = 4.dp"))
        assertTrue(toolbar.contains("TopAppBar("))
        assertFalse(toolbar.contains("CenterAlignedTopAppBar("))
        assertTrue(toolbar.contains("containerColor = Color.Transparent"))
        assertTrue(toolbar.contains("windowInsets = if (showBackButton)"))
        assertTrue(toolbar.contains("WindowInsets(0, 0, 0, 0)"))
        assertTrue(toolbar.contains("shape = RoundedCornerShape(24.dp)"))
        assertTrue(toolbar.contains("color = MaterialTheme.colorScheme.surfaceContainerHigh"))
        assertTrue(toolbar.contains("size = 32.dp"))
        assertTrue(toolbar.contains("Column(modifier = Modifier.widthIn(max = 148.dp))"))
        assertFalse(toolbar.contains("weight(1f, fill = false)"))
        assertTrue(toolbar.contains("\"title left=\$left top=\$top right=\$right bottom=\$bottom\""))
    }

    @Test
    fun selectionToolbarUsesConnectedActionsAndExpressiveSelectionShapes() {
        val toolbar = chat.substringAfter("private data class SelectionToolbarAction(")
            .substringBefore("internal fun ChatSearchBar(")

        assertTrue(toolbar.contains("ConnectedSelectionActionButton("))
        assertTrue(toolbar.contains("selected = false"))
        assertTrue(toolbar.contains("collectIsPressedAsState()"))
        assertTrue(toolbar.contains("Arrangement.spacedBy(ConnectedButtonGroupLayout.GAP)"))
        assertTrue(toolbar.contains("color = MaterialTheme.colorScheme.primary"))
        assertTrue(toolbar.contains("contentColor = MaterialTheme.colorScheme.onPrimary"))
        assertTrue(toolbar.contains("shape = MaterialShapes.Gem.toShape()"))
        assertTrue(toolbar.contains("shape = MaterialShapes.Cookie6Sided.toShape()"))
        assertTrue(toolbar.contains("actions.forEachIndexed"))
        assertFalse(toolbar.contains("IconButton("))
        assertFalse(toolbar.contains("TopAppBar("))
    }

    @Test
    fun callAndOverflowRemainASplitButtonWhileTheMenuUsesFabPills() {
        val fabMenuItem = chat.substringAfter("private fun ChatFabMenuItem(")
            .substringBefore("internal fun ChatTopBar(")
        val toolbar = chat.substringAfter("internal fun ChatTopBar(")
            .substringBefore("internal fun ChatMessageList(")

        val call = toolbar.indexOf("Icons.Rounded.Call")
        val overflow = toolbar.indexOf("Icons.Rounded.MoreVert")
        val menu = toolbar.indexOf("DropdownMenu(")
        assertTrue(call >= 0 && call < overflow && overflow < menu)
        assertTrue(toolbar.contains("SplitButtonLayout("))
        assertTrue(toolbar.contains("SplitButtonDefaults.TonalLeadingButton("))
        assertTrue(toolbar.contains("SplitButtonDefaults.TonalTrailingButton("))
        assertTrue(toolbar.contains("checked = menuOpen"))
        assertTrue(toolbar.contains("onCheckedChange = { onMenuToggle() }"))
        assertTrue(toolbar.contains("SplitButtonDefaults.LeadingIconSize"))
        assertTrue(toolbar.contains("SplitButtonDefaults.TrailingIconSize"))
        assertTrue(toolbar.contains("expanded = menuOpen"))
        assertTrue(toolbar.contains("containerColor = Color.Transparent"))
        assertTrue(toolbar.contains("shadowElevation = 0.dp"))
        assertTrue(toolbar.contains("Arrangement.spacedBy(4.dp)"))
        assertTrue(toolbar.contains("ChatFabMenuItem("))
        assertFalse(toolbar.contains("DropdownMenuItem("))
        assertTrue(toolbar.contains("val backdropRoot = LocalView.current.rootView"))
        assertTrue(toolbar.contains("var itemOrder = 0"))
        assertTrue(toolbar.split("order = itemOrder++").size - 1 >= 7)
        assertTrue(fabMenuItem.contains("LocalReduceMotion.current"))
        assertTrue(fabMenuItem.contains("delay(order * 45L)"))
        assertTrue(fabMenuItem.contains("val widthProgress by animateFloatAsState("))
        assertTrue(fabMenuItem.contains("val alphaProgress by animateFloatAsState("))
        assertTrue(fabMenuItem.contains(".clipToBounds()"))
        assertTrue(fabMenuItem.contains(".layout { measurable, constraints ->"))
        assertTrue(fabMenuItem.contains("placeable.place(width - placeable.width, 0)"))
        assertTrue(fabMenuItem.contains("DynamicBackdropBlur("))
        assertTrue(fabMenuItem.contains("blurRadius = 32.dp"))
        assertTrue(fabMenuItem.contains("shape = CircleShape"))
        assertTrue(fabMenuItem.contains("color = Color.Transparent"))
        assertTrue(fabMenuItem.contains("LocalTranslucentSurfaceAlpha.current"))
        assertTrue(fabMenuItem.contains("MaterialTheme.colorScheme.primaryContainer.copy("))
        assertTrue(fabMenuItem.contains("alpha = surfaceAlpha"))
        assertTrue(fabMenuItem.contains("contentColor = MaterialTheme.colorScheme.onPrimaryContainer"))
        assertTrue(fabMenuItem.contains(".height(56.dp)"))
        assertTrue(fabMenuItem.contains(".padding(horizontal = 24.dp)"))
        assertTrue(fabMenuItem.contains("Arrangement.spacedBy(8.dp)"))
        assertFalse(fabMenuItem.contains("Modifier.blur("))
        assertTrue(chat.contains("onMenuToggle = { menuOpen = !menuOpen }"))
        assertTrue(toolbar.contains("\"split left=\$left top=\$top right=\$right bottom=\$bottom\""))
        assertTrue(versions.contains("material3 = \"1.5.0\""))
        assertTrue(
            versions.contains(
                "androidx-material3 = { group = \"androidx.compose.material3\", " +
                    "name = \"material3\", version.ref = \"material3\" }"
            )
        )
    }

    @Test
    fun material3SplitButtonCompilesAgainstItsRequiredAndroidApi() {
        assertTrue(appBuild.contains("compileSdk = 37"))
        assertTrue(mmsBuild.contains("compileSdk = 37"))
        assertTrue(appBuild.contains("targetSdk = 36"))
    }

    @Test
    fun previewsCoverClosedAndOpenHeaderMenuStates() {
        val preview = previews.substringAfter("private fun ChatTopBarPreview()")
            .substringBefore("private fun ChatSearchBarPreview()")

        assertTrue(preview.contains("menuOpen = false"))
        assertTrue(preview.contains("menuOpen = true"))
        assertTrue(preview.split("FloatingChatHeader {").size - 1 >= 2)
    }

    @Test
    fun composerGroupsTheInputAndVoiceButtonInsideARoundedRectangle() {
        val composer = chat.substringAfter("internal fun InputBar(")
            .substringBefore("internal fun SuggestionChips(")

        assertTrue(composer.contains(".padding(horizontal = 12.dp, vertical = 8.dp)"))
        assertTrue(composer.contains("shape = RoundedCornerShape(28.dp)"))
        assertTrue(composer.contains("color = MaterialTheme.colorScheme.inputPill"))
        assertTrue(composer.contains("shape = RoundedCornerShape(32.dp)"))
        assertTrue(composer.contains("color = MaterialTheme.colorScheme.surfaceContainer,"))
        assertTrue(composer.contains("shadowElevation = 4.dp"))
        assertTrue(composer.contains("modifier = Modifier.fillMaxWidth().padding(4.dp)"))
    }

    @Test
    fun messagesUseTheFullViewportBehindTheFloatingChrome() {
        val screen = chat.substringAfter("fun ChatScreen(")
            .substringBefore("internal fun FloatingChatHeader(")
        val bottomBar = screen.substringAfter("bottomBar = {")
            .substringBefore(") { padding ->")

        assertTrue(screen.contains(") { padding ->\n        Box(Modifier.fillMaxSize())"))
        assertFalse(screen.contains("Box(Modifier.fillMaxSize().padding(padding))"))
        assertTrue(screen.contains("top = padding.calculateTopPadding() + 12.dp"))
        assertTrue(screen.contains("bottom = padding.calculateBottomPadding() + 12.dp"))
        assertTrue(bottomBar.contains("Modifier.navigationBarsPadding()"))
        assertFalse(
            bottomBar.substringBefore("AnimatedVisibility(").contains(".background(")
        )
    }

    @Test
    fun conversationSheetDoesNotShowARedundantBackButton() {
        assertTrue(activity.contains("showBackButton = !chatUsesSheet"))
        assertTrue(chat.contains("if (showBackButton) {\n                IconButton(onClick = onBack)"))
        assertFalse(activity.contains("showBackButton = false"))
    }

    @Test
    fun floatingBoundsRemainObservableForDeviceRegression() {
        assertTrue(chat.contains("\"header left=\$left top=\$top right=\$right bottom=\$bottom\""))
        assertTrue(chat.contains("\"composer left=\$left top=\$top right=\$right bottom=\$bottom\""))
        assertTrue(chat.contains("\"messages top=\$top bottom=\$bottom\""))
    }
}
