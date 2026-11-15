package vn.io.elsutm.mbqr.handoff

import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserCatalogTest {

    private val selfPackage = "vn.io.elsutm.mbqr"

    @Test
    fun `chooser comes first and installed browsers are sorted by label`() {
        val options = BrowserCatalog.merge(
            candidates = listOf(
                BrowserCatalog.Candidate("com.brave.browser", "Brave"),
                BrowserCatalog.Candidate("com.android.chrome", "chrome"),
                BrowserCatalog.Candidate("org.mozilla.firefox", "Firefox"),
            ),
            selectedPackage = null,
            selfPackage = selfPackage,
        )

        assertEquals(
            listOf(
                BrowserCatalog.chooser,
                BrowserOption("com.brave.browser", "Brave"),
                BrowserOption("com.android.chrome", "chrome"),
                BrowserOption("org.mozilla.firefox", "Firefox"),
            ),
            options,
        )
    }

    @Test
    fun `duplicates and the app itself are dropped`() {
        val options = BrowserCatalog.merge(
            candidates = listOf(
                BrowserCatalog.Candidate("com.android.chrome", "Chrome"),
                BrowserCatalog.Candidate("com.android.chrome", "Google Chrome"),
                BrowserCatalog.Candidate(selfPackage, "MBQR"),
            ),
            selectedPackage = null,
            selfPackage = selfPackage,
        )

        assertEquals(
            listOf(
                BrowserCatalog.chooser,
                BrowserOption("com.android.chrome", "Chrome"),
            ),
            options,
        )
    }

    @Test
    fun `installed selection is not repeated`() {
        val options = BrowserCatalog.merge(
            candidates = listOf(BrowserCatalog.Candidate("com.android.chrome", "Chrome")),
            selectedPackage = "com.android.chrome",
            selfPackage = selfPackage,
        )

        assertEquals(2, options.size)
    }

    @Test
    fun `missing selection is appended as a placeholder`() {
        val options = BrowserCatalog.merge(
            candidates = listOf(BrowserCatalog.Candidate("com.android.chrome", "Chrome")),
            selectedPackage = "com.example.removed",
            selfPackage = selfPackage,
        )

        assertEquals(
            BrowserOption("com.example.removed", "Trình duyệt đã chọn"),
            options.last(),
        )
    }

    @Test
    fun `empty candidate list still offers the chooser`() {
        assertEquals(
            listOf(BrowserCatalog.chooser),
            BrowserCatalog.merge(emptyList(), selectedPackage = null, selfPackage = selfPackage),
        )
    }
}
