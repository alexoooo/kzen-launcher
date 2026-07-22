package tech.kzen.launcher.server.archetype

import com.google.common.io.MoreFiles
import com.google.common.io.RecursiveDeleteOption
import tech.kzen.launcher.server.service.DownloadService
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class ArchetypeRepoTest {
    //-----------------------------------------------------------------------------------------------------------------
    private val tempDirs = mutableListOf<Path>()

    private val archetypeHome = tempDir().resolve("kzen-archetypes")
    private val sourceDir = tempDir()


    @AfterTest
    fun tearDown() {
        for (dir in tempDirs) {
            MoreFiles.deleteRecursively(dir, RecursiveDeleteOption.ALLOW_INSECURE)
        }
    }


    private fun tempDir(): Path {
        val dir = Files.createTempDirectory("archetype-repo-test")
        tempDirs.add(dir)
        return dir
    }


    private fun repo(currentUrl: String, releasedUrl: String? = null): ArchetypeRepo {
        return ArchetypeRepo(
            DownloadService(),
            archetypeName = "kzen-project",
            title = "Automation and Reporting",
            descriptionBase = "Visually control a browser and more",
            currentUrl = currentUrl,
            releasedUrl = releasedUrl,
            archetypeHome = archetypeHome)
    }


    private fun source(artifactName: String, content: String): String {
        val path = sourceDir.resolve(artifactName)
        Files.writeString(path, content)
        return path.toUri().toString()
    }


    private fun cached(artifactName: String, content: String = artifactName): Path {
        Files.createDirectories(archetypeHome)
        val path = archetypeHome.resolve(artifactName)
        Files.writeString(path, content)
        return path
    }


    private fun cachedContent(artifactName: String): String {
        return Files.readString(archetypeHome.resolve(artifactName))
    }


    //-----------------------------------------------------------------------------------------------------------------
    @Test
    fun `scan derives per-version entries with version in description`() {
        cached("kzen-project-0.29.1.zip")
        cached("kzen-project-0.28.0.zip")

        val all = repo(source("kzen-project-0.30.0-SNAPSHOT.zip", "unused")).all()

        val info = all["kzen-project-0.29.1"]!!
        assertEquals("Automation and Reporting", info.title)
        assertEquals("Visually control a browser and more - v0.29.1", info.description)
        assertEquals(archetypeHome.resolve("kzen-project-0.29.1.zip"), info.location)
        assertEquals("kzen-project", info.archetype)
        assertEquals("0.29.1", info.version)
        assertEquals("Visually control a browser and more - v0.28.0", all["kzen-project-0.28.0"]!!.description)
        assertEquals("0.28.0", all["kzen-project-0.28.0"]!!.version)
    }


    @Test
    fun `entries sorted version-descending, snapshot above equal release, unparseable last`() {
        cached("kzen-project-0.28.0.zip")
        cached("kzen-project-0.30.0.zip")
        cached("kzen-project-0.29.1.zip")
        cached("kzen-project-custom.zip")
        cached("kzen-project-0.30.0-SNAPSHOT.zip")

        val names = repo(source("kzen-project-0.30.0-SNAPSHOT.zip", "unused")).all().keys.toList()

        assertContentEquals(
            listOf(
                "kzen-project-0.30.0-SNAPSHOT",
                "kzen-project-0.30.0",
                "kzen-project-0.29.1",
                "kzen-project-0.28.0",
                "kzen-project-custom"),
            names)
    }


    @Test
    fun `empty or missing home scans empty`() {
        assertTrue(repo(source("kzen-project-0.30.0-SNAPSHOT.zip", "unused")).all().isEmpty())
    }


    @Test
    fun `get is a lookup, not a path resolve`() {
        cached("kzen-project-0.29.1.zip")
        val repo = repo(source("kzen-project-0.30.0-SNAPSHOT.zip", "unused"))

        assertEquals(archetypeHome.resolve("kzen-project-0.29.1.zip"), repo.get("kzen-project-0.29.1").location)
        assertFailsWith<IllegalArgumentException> { repo.get("no-such-archetype") }
        assertFailsWith<IllegalArgumentException> { repo.get("../../kzen-project-0.29.1") }
    }


    //-----------------------------------------------------------------------------------------------------------------
    @Test
    fun `current file source re-acquired on every init`() {
        val sourcePath = sourceDir.resolve("kzen-project-0.30.0-SNAPSHOT.zip")

        val repo = repo(source("kzen-project-0.30.0-SNAPSHOT.zip", "v1"))
        repo.init()
        assertEquals("v1", cachedContent("kzen-project-0.30.0-SNAPSHOT.zip"))

        Files.writeString(sourcePath, "v2")
        repo.init()
        assertEquals("v2", cachedContent("kzen-project-0.30.0-SNAPSHOT.zip"))
    }


    @Test
    fun `released source downloaded only if absent`() {
        val releasedUrl = source("kzen-project-0.29.1.zip", "r1")
        val repo = repo(source("kzen-project-0.30.0-SNAPSHOT.zip", "current"), releasedUrl)

        repo.init()
        assertEquals("r1", cachedContent("kzen-project-0.29.1.zip"))

        Files.writeString(sourceDir.resolve("kzen-project-0.29.1.zip"), "r2")
        repo.init()
        assertEquals("r1", cachedContent("kzen-project-0.29.1.zip"))
    }


    @Test
    fun `released matching cached artifact is not downloaded`() {
        cached("kzen-project-0.29.1.zip", "already cached")

        // The released URL is unreachable; the absence check must dedupe before any download.
        val unreachable = sourceDir.resolve("missing/kzen-project-0.29.1.zip").toUri().toString()
        repo(source("kzen-project-0.30.0-SNAPSHOT.zip", "current"), unreachable).init()

        assertEquals("already cached", cachedContent("kzen-project-0.29.1.zip"))
    }


    @Test
    fun `stale snapshots pruned when current acquired, releases kept`() {
        cached("kzen-project-0.29.1-SNAPSHOT.zip")
        cached("kzen-project-0.29.0.zip")

        repo(source("kzen-project-0.30.0-SNAPSHOT.zip", "current")).init()

        assertFalse(Files.exists(archetypeHome.resolve("kzen-project-0.29.1-SNAPSHOT.zip")))
        assertTrue(Files.exists(archetypeHome.resolve("kzen-project-0.29.0.zip")))
        assertTrue(Files.exists(archetypeHome.resolve("kzen-project-0.30.0-SNAPSHOT.zip")))
    }


    @Test
    fun `snapshots not pruned when current acquisition fails`() {
        cached("kzen-project-0.29.1-SNAPSHOT.zip")

        val unreachable = sourceDir.resolve("missing/kzen-project-0.30.0-SNAPSHOT.zip").toUri().toString()
        repo(unreachable).init()

        assertTrue(Files.exists(archetypeHome.resolve("kzen-project-0.29.1-SNAPSHOT.zip")))
    }


    @Test
    fun `init cleans legacy metadata and orphaned part files`() {
        cached("kzen-project-0.29.0.zip")
        Files.writeString(archetypeHome.resolve("kzen-archetypes.yaml"), "legacy")
        Files.writeString(archetypeHome.resolve("kzen-project-0.30.0.zip.part"), "orphan")

        repo(source("kzen-project-0.30.0-SNAPSHOT.zip", "current")).init()

        assertFalse(Files.exists(archetypeHome.resolve("kzen-archetypes.yaml")))
        assertFalse(Files.exists(archetypeHome.resolve("kzen-project-0.30.0.zip.part")))
        assertTrue(Files.exists(archetypeHome.resolve("kzen-project-0.29.0.zip")))
    }
}
