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
        assertTrue(sheet.contains("contentWindowInsets = { WindowInsets.ime }"))
        assertFalse(sheet.contains("contentWindowInsets = { WindowInsets(0, 0, 0, 0) }"))
        assertTrue(sheet.contains("bottomGap=\${windowHeightPx - bottom}"))
        assertFalse(sheet.contains("modifier = Modifier.fillMaxHeight"))
    }

    @Test
    fun sheetUsesDynamicClippedMaterialGlass() {
        val sheet = source("java/com/anindra/messages/ui/ConversationChatSheet.kt")
        val chat = source("java/com/anindra/messages/ui/ChatScreen.kt")
        val activity = source("java/com/anindra/messages/MainActivity.kt")
        val blur = source("java/com/anindra/messages/ui/DynamicBackdropBlur.kt")
        assertTrue(sheet.contains("LocalTranslucentSurfaceAlpha.current"))
        assertTrue(sheet.contains("SCRIM_ALPHA = 0.18f"))
        assertTrue(sheet.contains("DynamicBackdropBlur("))
        assertTrue(blur.contains("RenderEffect.createBlurEffect("))
        assertTrue(blur.contains("backgroundRoot.draw(canvas)"))
        assertTrue(blur.contains("AndroidView("))
        assertTrue(blur.contains("blurView.follow(samplePosition)"))
        assertTrue(blur.contains("getLocationOnScreen(ownLocation)"))
        assertTrue(sheet.contains(".height(sheetHeight)"))
        assertTrue(sheet.contains(".clip(sheetShape)"))
        assertTrue(sheet.contains("coordinates.positionInWindow()"))
        assertTrue(sheet.contains("containerColor = Color.Transparent"))
        assertFalse(blur.contains("FLAG_BLUR_BEHIND"))
        assertFalse(blur.contains("setBackgroundBlurRadius"))
        assertTrue(sheet.contains("alpha = surfaceAlpha"))
        assertTrue(activity.contains("translucentBackground = chatUsesSheet"))
        assertTrue(chat.contains("if (translucentBackground)"))
        assertTrue(chat.contains("Color.Transparent"))
    }

    @Test
    fun sameWindowGlassNeverRedrawsARecordingRenderNode() {
        val blur = source("java/com/anindra/messages/ui/DynamicBackdropBlur.kt")
        assertTrue(blur.contains("backgroundRoot.rootView === rootView"))
        assertTrue(blur.contains("scheduleSameWindowCapture()"))
        assertTrue(blur.contains("post {"))
        assertTrue(blur.contains("Bitmap.createBitmap("))
        assertFalse(blur.contains("PixelCopy.request("))
        assertTrue(blur.contains("captureSameWindowWithoutGlass("))
        assertTrue(blur.contains("val captureCanvas = Canvas(next)"))
        assertTrue(blur.contains("backgroundRoot.draw(captureCanvas)"))
        assertTrue(blur.contains("snapshot?.let { canvas.drawBitmap"))
        assertTrue(blur.contains("catch (_: IllegalStateException)"))
        val capture = blur.substringAfter("private fun captureSameWindowWithoutGlass")
            .substringBefore("private fun completeSameWindowCapture")
        assertTrue(capture.contains("captureGuard?.active = true"))
        assertTrue(capture.contains("captureGuard?.active = false"))
        assertTrue(
            capture.indexOf("captureGuard?.active = true") <
                capture.indexOf("backgroundRoot.draw(captureCanvas)")
        )
        assertTrue(
            capture.indexOf("backgroundRoot.draw(captureCanvas)") <
                capture.indexOf("captureGuard?.active = false")
        )
        val onDraw = blur.substringAfter("override fun onDraw(canvas: Canvas)")
        assertTrue(
            onDraw.indexOf("if (sharesWindowWithBackdrop())") <
                onDraw.indexOf("backgroundRoot.draw(canvas)")
        )
    }

    @Test
    fun compactSheetOwnsTheKeyboardInsetExactlyOnce() {
        val activity = source("java/com/anindra/messages/MainActivity.kt")
        val chat = source("java/com/anindra/messages/ui/ChatScreen.kt")
        assertTrue(activity.contains("imeInsetsHandledByParent = chatUsesSheet && !useNewUi"))
        assertTrue(activity.contains("bottomOverlayClearance = if (chatUsesSheet && useNewUi)"))
        assertTrue(activity.contains("topLevelControlsClearance"))
        assertTrue(chat.contains("if (imeInsetsHandledByParent)"))
        assertTrue(chat.contains("val bottomOverlayInset = if (imeBottom > navBottom) 0.dp"))
        assertTrue(chat.contains("composerBottomInset = keyboardInset"))
    }

    @Test
    fun compactChatKeepsTheListBehindTheSheet() {
        val activity = source("java/com/anindra/messages/MainActivity.kt")
        assertTrue(activity.contains("val chatUsesSheet = !twoPaneLayout && !bubbleMode"))
        assertTrue(activity.contains("target == \"chat\" && chatUsesSheet"))
        assertTrue(activity.contains("onDismissed = leaveChat"))
        assertTrue(activity.contains("onGestureDismiss = leaveChatByGesture"))
        assertTrue(activity.contains("chatContent(requestDismiss)"))
        assertTrue(activity.contains("PersistentConversationSheet("))
        assertTrue(activity.contains("conversationOpen = navRoute == \"chat\""))
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
        assertTrue(sheet.contains("DynamicBackdropBlur("))
        assertTrue(sheet.contains("actionSheetPosition"))
        assertTrue(sheet.contains("coordinates.positionInWindow()"))
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
        assertTrue(activity.contains("\"chat\" -> chatContent(leaveChat)"))
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
