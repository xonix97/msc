package org.discnest.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoundationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun launchesFoundationWithoutAdvertisingPlayback() {
        compose.onNodeWithText("DiscNest").assertIsDisplayed()
        compose.onNodeWithText("Music playback is not implemented in this foundation build.").assertIsDisplayed()
    }
}
