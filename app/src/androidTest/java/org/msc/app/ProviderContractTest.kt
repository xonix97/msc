package org.msc.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.msc.app.providers.ClashFlacCatalog

@RunWith(AndroidJUnit4::class)
class ProviderContractTest {
    @Test fun parsesDocumentedMetadataWithoutTreatingCatalogUrlAsAudio() {
        val provider = ClashFlacCatalog()
        val tracks = provider.parse("""[{"asin":"original-test","title":"Original fixture","artist":"msc","album":"Tests","duration_sec":3,"url":"https://example.org/catalog/item"}]""")
        assertEquals("Original fixture", tracks.single().title)
        assertEquals(3, tracks.single().durationSeconds)
        assertThrows(IllegalArgumentException::class.java) { provider.validateBaseUrl("http://insecure.example") }
        assertThrows(IllegalArgumentException::class.java) { provider.validateBaseUrl("https://user:password@example.org") }
        assertEquals("https://example.org", provider.validateBaseUrl("https://example.org/"))
    }
}
