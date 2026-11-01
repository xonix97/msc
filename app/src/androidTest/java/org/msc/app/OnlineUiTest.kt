package org.msc.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.msc.app.providers.OnlineSong
import org.msc.app.ui.OnlineSearchResults

@RunWith(AndroidJUnit4::class)
class OnlineUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun songRowsPlayQueueAndKeepUnavailableSongsDisabled() {
        val playable = OnlineSong("original", "Original test song", "msc", "Tests", 3000, "", "https://aac.saavncdn.com/test.mp4", "160kbps")
        val unavailable = playable.copy(id = "none", title = "Unavailable test song", streamUrl = null)
        var played = ""; var queued = ""
        compose.setContent { MaterialTheme {
            OnlineSearchResults(OnlineUi(active = true, results = listOf(playable, unavailable)), "test", {},
                { played = it.id }, { queued = it.id }, {})
        } }
        compose.onNodeWithText("Original test song").performClick()
        compose.runOnIdle { assertEquals("original", played) }
        compose.onNodeWithContentDescription("Queue Original test song").performClick()
        compose.runOnIdle { assertEquals("original", queued) }
        compose.onNodeWithContentDescription("Queue Unavailable test song").assertIsNotEnabled()
        compose.onNodeWithText("Unavailable to stream").assertIsDisplayed()
    }
    @Test fun offlineStateOffersWorkingRetryWithoutFakeSongs() {
        var retried = false
        compose.setContent { MaterialTheme {
            OnlineSearchResults(OnlineUi(active = true, error = "You’re offline."), "test", { retried = true }, {}, {}, {})
        } }
        compose.onNodeWithText("You’re offline.").assertIsDisplayed()
        compose.onNodeWithText("Retry search").performClick()
        compose.runOnIdle { assertTrue(retried) }
    }
}
