package org.msc.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoundationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun launchesRealLibraryAndNavigates() {
        compose.onNodeWithText("msc").assertIsDisplayed()
        compose.onNodeWithText("Music playback is not implemented in this foundation build.").assertDoesNotExist()
        compose.onNodeWithText("Import files").assertIsDisplayed()
        compose.onNodeWithText("Library").performClick()
        compose.onNodeWithText("Your library").assertIsDisplayed()
        compose.onNodeWithText("Search").performClick()
        compose.onNodeWithText("Search music").assertIsDisplayed()
    }
}
