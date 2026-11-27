package com.anindra.messages.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ConversationChatSheetTest {

    private val main = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "app/src/main") }
        .firstOrNull { it.isDirectory }
        ?: error("app/src/main not found")

    private fun source(path: String) = File(main, path).readText()

    @Test
    fun sheetStopsAtNinetyPercent() {
        assertEquals(0.9f, ChatSheetLayout.HEIGHT_FRACTION, 0f)
    }

    @Test
    fun sheetIsMaterialThreeAndCanBeDraggedClosed() {
        val sheet = source("java/com/anindra/messages/ui/ConversationChatSheet.kt")
        assertTrue(sheet.contains("ModalBottomSheet("))
        assertTrue(sheet.contains("skipPartiallyExpanded = true"))
        assertTrue(sheet.contains("BoxWithConstraints(Modifier.fillMaxSize())"))
        assertTrue(sheet.contains("val sheetHeight = maxHeight * ChatSheetLayout.HEIGHT_FRACTION"))
        assertTrue(sheet.contains(".height(sheetHeight)"))
        assertTrue(sheet.contains("sheetState.hide()"))
        assertTrue(sheet.contains("dragHandle = null"))
        assertTrue(sheet.contains("BottomSheetDefaults.DragHandle()"))
        assertTrue(sheet.contains(".height(ChatSheetLayout.DRAG_HANDLE_HEIGHT)"))
        assertTrue(sheet.contains("RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)"))
        assertTrue(sheet.contains("contentWindowInsets = { WindowInsets(0, 0, 0, 0) }"))
        assertFalse(sheet.contains("contentWindowInsets = { WindowInsets.ime }"))
        assertTrue(sheet.contains(".windowInsetsPadding(WindowInsets.ime)"))
        assertTrue(sheet.contains("bottomGap=\${windowHeightPx - bottom}"))
        assertFalse(sheet.contains("modifier = Modifier.fillMaxHeight"))
    }

    @Test
    fun sheetUsesDynamicClippedMaterialGlass() {
        val sheet = source("java/com/anindra/messages/ui/ConversationChatSheet.kt")
        val chat = source("java/com/anindra/messages/ui/ChatScreen.kt")
        val activity = source("java/com/anindra/messages/MainActivity.kt")
        val blur = source("java/com/anindra/messages/ui/DynamicBackdropBlur.kt")
        val manifest = source("AndroidManifest.xml")
        assertTrue(sheet.contains("LocalTranslucentSurfaceAlpha.current"))
        assertTrue(sheet.contains("SCRIM_ALPHA = 0.18f"))
        assertTrue(sheet.contains(".dynamicBackdropEffect("))
        assertTrue(sheet.contains("ChatSheetLayout.BACKGROUND_BLUR_RADIUS"))
        assertTrue(sheet.contains("DynamicBackdropLayer.SHEET"))
        assertTrue(blur.contains("rememberLayerBackdrop("))
        assertTrue(blur.contains("graph.registerSource(owner, layer, backdrop)"))
        assertTrue(blur.contains("graph.unregisterSource(owner)"))
        assertTrue(blur.contains("layerBackdrop(backdrop)"))
        assertTrue(blur.contains("drawBackdrop("))
        assertTrue(blur.contains("textureBlurEffect("))
        assertTrue(blur.contains("Build.VERSION_CODES.TIRAMISU"))
        assertTrue(blur.contains("if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {\n        content()"))
        assertTrue(
            manifest.contains(
                "tools:overrideLibrary=\"top.yukonga.miuix.kmp.blur\""
            )
        )
        assertTrue(blur.contains("internal fun DynamicBackdropMotionTracker("))
        assertTrue(blur.contains("graph.motionFrame"))
        assertTrue(blur.contains("withFrameNanos { }"))
        assertTrue(blur.contains("graph.advanceMotionFrame()"))
        assertTrue(sheet.contains("DynamicBackdropMotionTracker {"))
        assertTrue(sheet.contains("sheetState.requireOffset()"))
        assertTrue(sheet.contains(".height(sheetHeight)"))
        assertTrue(sheet.contains(".clip(sheetShape)"))
        assertTrue(sheet.contains("coordinates.positionInWindow()"))
        assertTrue(sheet.contains("containerColor = Color.Transparent"))
        assertTrue(sheet.contains("alpha = surfaceAlpha"))
        assertTrue(activity.contains("DynamicBackdropHost {"))
        assertTrue(activity.contains("translucentBackground = chatUsesSheet"))
        assertTrue(chat.contains("if (translucentBackground)"))
        assertTrue(chat.contains("Color.Transparent"))
    }

    @Test
    fun liveGlassDoesNotCaptureRootBitmapsAfterTheFrame() {
        val blur = source("java/com/anindra/messages/ui/DynamicBackdropBlur.kt")
        assertFalse(blur.contains("AndroidView("))
        assertFalse(blur.contains("Bitmap.createBitmap("))
        assertFalse(blur.contains("backgroundRoot.draw("))
        assertFalse(blur.contains("post {"))
        assertFalse(blur.contains("samplePosition"))
        assertTrue(blur.contains("One shared, hardware-accelerated LayerBackdrop graph"))
        assertTrue(blur.contains("live layout coordinates"))
        assertFalse(blur.contains("dev.chrisbanes.haze"))
    }

    @Test
    fun persistentComposerDoesNotReserveTheCoveredNavigation() {
        val activity = source("java/com/anindra/messages/MainActivity.kt")
        val chat = source("java/com/anindra/messages/ui/ChatScreen.kt")
        assertTrue(activity.contains("imeInsetsHandledByParent = chatUsesSheet && !useNewUi"))
        assertFalse(activity.contains("bottomOverlayClearance = if (chatUsesSheet && useNewUi)"))
        assertFalse(chat.contains("bottomOverlayClearance"))
        assertFalse(chat.contains("bottomOverlayInset"))
        assertTrue(chat.contains("Column(Modifier.navigationBarsPadding())"))
        assertTrue(chat.contains("if (imeInsetsHandledByParent)"))
        assertTrue(chat.contains("composerBottomInset = keyboardInset"))
    }

    @Test
    fun compactChatKeepsTheListBehindTheSheet() {
        val activity = source("java/com/anindra/messages/MainActivity.kt")
        assertTrue(activity.contains("val chatUsesSheet = !twoPaneLayout && !bubbleMode"))
        assertTrue(activity.contains("target == \"chat\" && chatUsesSheet"))
        assertTrue(activity.contains("onDismissed = leaveChat"))
        assertTrue(activity.contains("onGestureDismiss = leaveChatByGesture"))
        assertTrue(activity.contains("chatContent(requestDismiss, true)"))
        assertTrue(activity.contains("PersistentConversationSheet("))
        assertTrue(activity.contains("conversationOpen = persistentConversationOpen"))
        assertTrue(activity.contains("onCollapsed = leaveChatByGesture"))
        assertTrue(activity.contains("val showTopLevelNavigation = useNewUi"))
        assertTrue(activity.contains("(chatUsesSheet && navRoute == \"chat\")"))
        assertTrue(activity.contains("showNavigationBar = showTopLevelNavigation"))
        assertTrue(activity.contains("if (showTopLevelNavigation)"))
        assertTrue(
            activity.contains(
                "chatUsesSheet &&\n                                        " +
                    "(initialState == \"chat\" || targetState == \"chat\")"
            )
        )
    }

    @Test
    fun longPressActionSheetUsesTheSameDynamicGlass() {
        val conversations = source("java/com/anindra/messages/ui/ConversationsScreen.kt")
        val sheet = conversations.substringAfter("if (sheetConvo != null)")
            .substringBefore("permanentDeleteTarget?.let")
        assertTrue(sheet.contains("containerColor = Color.Transparent"))
        assertTrue(sheet.contains(".dynamicBackdropEffect("))
        assertTrue(sheet.contains("DynamicBackdropLayer.SHEET"))
        assertTrue(sheet.contains("followMotion = true"))
        assertFalse(sheet.contains(".dynamicBackdropSource(DynamicBackdropLayer.SHEET)"))
        assertFalse(sheet.contains("actionSheetPosition"))
        assertTrue(conversations.contains("LocalTranslucentSurfaceAlpha.current"))
        assertTrue(sheet.contains("alpha = translucentSurfaceAlpha"))
        assertTrue(sheet.contains("BottomSheetDefaults.DragHandle()"))
    }

    @Test
    fun startChatOpensTheContactPickerInTheSameGlassSheet() {
        val activity = source("java/com/anindra/messages/MainActivity.kt")
        val newChat = source("java/com/anindra/messages/ui/NewChatScreen.kt")
        assertTrue(activity.contains("val newChatUsesSheet = !twoPaneLayout && !bubbleMode"))
        assertTrue(activity.contains("newChatUsesSheet && navRoute == \"new\""))
        assertTrue(activity.contains("newChatContent(requestDismiss)"))
        assertTrue(activity.contains("onPick = { address, name ->"))
        assertTrue(activity.contains("vm.openOrCreate(address, name)"))
        assertTrue(activity.contains("navRoute = \"chat\""))
        assertTrue(newChat.contains("showBackButton: Boolean = true"))
        assertTrue(newChat.contains("translucentBackground: Boolean = false"))
        assertTrue(newChat.contains("if (translucentBackground)"))
    }

    @Test
    fun expandedAndBubbleChatsKeepTheirDedicatedPane() {
        val activity = source("java/com/anindra/messages/MainActivity.kt")
        assertTrue(activity.contains("\"chat\" -> chatContent(leaveChat, true)"))
    }

    @Test
    fun aSwipeDismissStillPersistsTheLatestDraft() {
        val activity = source("java/com/anindra/messages/MainActivity.kt")
        val chat = source("java/com/anindra/messages/ui/ChatScreen.kt")
        assertTrue(chat.contains("SideEffect { onDraftSnapshot(draft) }"))
        assertTrue(activity.contains("vm.saveDraftAndMaybeTrash(chatId, chatDraftSnapshot.trim())"))
        assertTrue(activity.contains("onGestureDismiss = leaveChatByGesture"))
    }
}
