package tech.kzen.launcher.common.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


// Pins the ordering semantics ArchetypeRepo used to own (its `scan derives ...` / sort tests), now that the
//  client depends on them too for the upgrade offer. Behaviour must stay byte-identical to that extraction.
class VersionNumbersTest {
    //-----------------------------------------------------------------------------------------------------------------
    @Test
    fun `descending sort matches the archetype catalogue order`() {
        val sorted = listOf(
            "0.28.0", "0.30.0", "0.29.1", "custom", "0.30.0-SNAPSHOT")
            .sortedWith(Comparator { a, b -> VersionNumbers.compare(b, a) })

        // snapshot above equal release, then descending releases, then unparseable last
        assertEquals(
            listOf("0.30.0-SNAPSHOT", "0.30.0", "0.29.1", "0.28.0", "custom"),
            sorted)
    }


    @Test
    fun `snapshot sorts above its equal release`() {
        assertTrue(VersionNumbers.compare("0.30.0-SNAPSHOT", "0.30.0") > 0)
        assertTrue(VersionNumbers.compare("0.30.0", "0.30.0-SNAPSHOT") < 0)
    }


    @Test
    fun `newer release is greater`() {
        assertTrue(VersionNumbers.compare("0.30.0", "0.29.1") > 0)
        assertTrue(VersionNumbers.compare("0.29.1", "0.30.0") < 0)
        assertTrue(VersionNumbers.compare("1.0.0", "0.99.99") > 0)
    }


    @Test
    fun `equal versions compare equal`() {
        assertEquals(0, VersionNumbers.compare("0.30.0", "0.30.0"))
        assertEquals(0, VersionNumbers.compare("0.30.0-SNAPSHOT", "0.30.0-SNAPSHOT"))
    }


    @Test
    fun `differing component counts compare by shared prefix`() {
        assertTrue(VersionNumbers.compare("0.30.1", "0.30") > 0)
        assertEquals(0, VersionNumbers.compare("0.30.0", "0.30"))
    }


    @Test
    fun `unparseable versions sort below every parseable version`() {
        assertTrue(VersionNumbers.compare("0.0.1", "custom") > 0)
        assertTrue(VersionNumbers.compare("custom", "0.0.1") < 0)
    }


    @Test
    fun `parses distinguishes numeric versions from unparseable`() {
        assertTrue(VersionNumbers.parses("0.30.0"))
        assertTrue(VersionNumbers.parses("0.30.0-SNAPSHOT"))
        assertFalse(VersionNumbers.parses("custom"))
        assertFalse(VersionNumbers.parses("0.x.0"))
    }


    @Test
    fun `isSnapshot detects the snapshot suffix`() {
        assertTrue(VersionNumbers.isSnapshot("0.30.0-SNAPSHOT"))
        assertFalse(VersionNumbers.isSnapshot("0.30.0"))
    }
}
