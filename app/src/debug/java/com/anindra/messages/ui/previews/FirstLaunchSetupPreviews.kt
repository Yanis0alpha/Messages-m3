package com.anindra.messages.ui.previews

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.anindra.messages.ui.FirstLaunchSetupScreen
import com.anindra.messages.ui.SetupRequirementCard
import com.anindra.messages.ui.theme.MessagesTheme

@PreviewLightDark
@Composable
private fun FirstLaunchSetupScreenPreview() {
    MessagesTheme(mode = "system") {
        FirstLaunchSetupScreen(
            defaultSmsReady = false,
            permissionsReady = false,
            onRequestDefaultSms = {},
            onRequestPermissions = {},
            onComplete = {},
            onNotNow = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun SetupRequirementCardPreview() {
    MessagesTheme(mode = "system") {
        Box(Modifier.padding(24.dp)) {
            SetupRequirementCard(
                icon = Icons.Rounded.Notifications,
                title = "Essential permissions",
                subtitle = "SMS access and notifications",
                complete = false,
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
