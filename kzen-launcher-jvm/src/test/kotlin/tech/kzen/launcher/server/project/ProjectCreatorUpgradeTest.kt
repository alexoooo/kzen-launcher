package tech.kzen.launcher.server.project

import com.google.common.io.MoreFiles
import com.google.common.io.RecursiveDeleteOption
import tech.kzen.launcher.server.archetype.ArchetypeInfo
import tech.kzen.launcher.server.archetype.ArchetypeRepo
import tech.kzen.launcher.server.service.DownloadService
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class ProjectCreatorUpgradeTest {
    //-----------------------------------------------------------------------------------------------------------------
    private val tempDirs = mutableListOf<Path>()

    private val projectHome = tempDir()
    private val sourceDir = tempDir()


    @AfterTest
    fun tearDown() {
        for (dir in tempDirs) {
            MoreFiles.deleteRecursively(dir, RecursiveDeleteOption.ALLOW_INSECURE)
        }
    }


    private fun tempDir(): Path {
        val dir = Files.createTempDirectory("project-creator-upgrade-test")
        tempDirs.add(dir)
        return dir
    }


    private fun creator(): ProjectCreator {
        // upgrade() does not touch the archetype repo; a minimal one satisfies the constructor.
        val archetypeRepo = ArchetypeRepo(
            DownloadService(),
            archetypeName = "kzen-project",
            title = "t",
            descriptionBase = "d",
            currentUrl = sourceDir.resolve("unused.zip").toUri().toString(),
            releasedUrl = null,
            archetypeHome = sourceDir.resolve("archetypes"))
        return ProjectCreator(archetypeRepo, projectHome)
    }


    // A project home as the launcher would have created it: the program (main.jar + dependencies/) plus the
    //  user's own data (notation/, work/, logs/, and a stray file) that an upgrade must never touch.
    private fun createdProjectHome(name: String, version: String): Path {
        val home = Files.createDirectories(projectHome.resolve(name))
        Files.writeString(home.resolve("main.jar"), "main-$version")
        val deps = Files.createDirectories(home.resolve("dependencies"))
        Files.writeString(deps.resolve("dep-$version.jar"), "dep-$version")

        Files.createDirectories(home.resolve("notation/main"))
        Files.writeString(home.resolve("notation/main/User.yaml"), "user notation")
        Files.createDirectories(home.resolve("logs"))
        Files.writeString(home.resolve("logs/run.log"), "log")
        Files.createDirectories(home.resolve("work"))
        Files.writeString(home.resolve("work/state.tmp"), "work")
        Files.writeString(home.resolve("user-notes.txt"), "notes")
        return home
    }


    // An archetype zip in the shape ProjectCreator.upgrade unzips: main.jar + dependencies/ + a seed notation
    //  document (which upgrade must NOT import into an existing project).
    private fun archetype(version: String, includeMainJar: Boolean = true): ArchetypeInfo {
        val zipPath = sourceDir.resolve("kzen-project-$version.zip")
        ZipOutputStream(Files.newOutputStream(zipPath)).use { zip ->
            if (includeMainJar) {
                zip.putNextEntry(ZipEntry("main.jar"))
                zip.write("main-$version".toByteArray())
                zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry("dependencies/dep-$version.jar"))
            zip.write("dep-$version".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("src/main/resources/notation/main/Seed.yaml"))
            zip.write("archetype seed $version".toByteArray())
            zip.closeEntry()
        }
        return ArchetypeInfo("t", "d", zipPath, "kzen-project", version)
    }


    private fun isWindows(): Boolean {
        return System.getProperty("os.name").lowercase().contains("win")
    }


    //-----------------------------------------------------------------------------------------------------------------
    @Test
    fun `upgrade replaces the program and preserves user data`() {
        val home = createdProjectHome("alpha", "0.29.1")

        creator().upgrade(home, archetype("0.30.0"))

        // main.jar + dependencies/ swapped to the new version
        assertEquals("main-0.30.0", Files.readString(home.resolve("main.jar")))
        assertTrue(Files.exists(home.resolve("dependencies/dep-0.30.0.jar")))
        assertFalse(Files.exists(home.resolve("dependencies/dep-0.29.1.jar")))

        // user data untouched
        assertEquals("user notation", Files.readString(home.resolve("notation/main/User.yaml")))
        assertEquals("log", Files.readString(home.resolve("logs/run.log")))
        assertEquals("work", Files.readString(home.resolve("work/state.tmp")))
        assertEquals("notes", Files.readString(home.resolve("user-notes.txt")))

        // the archetype's seed notation is NOT imported into an existing project
        assertFalse(Files.exists(home.resolve("src/main/resources/notation/main/Seed.yaml")))

        // no residue
        assertFalse(Files.exists(home.resolve("main.jar.old")))
        assertFalse(Files.exists(home.resolve("dependencies.old")))
        assertFalse(Files.exists(home.resolveSibling("alpha.upgrade")))
    }


    @Test
    fun `archetype missing main jar fails and leaves the home unchanged`() {
        val home = createdProjectHome("alpha", "0.29.1")

        assertFailsWith<IllegalStateException> {
            creator().upgrade(home, archetype("0.30.0", includeMainJar = false))
        }

        // home byte-identical, staging discarded
        assertEquals("main-0.29.1", Files.readString(home.resolve("main.jar")))
        assertTrue(Files.exists(home.resolve("dependencies/dep-0.29.1.jar")))
        assertFalse(Files.exists(home.resolveSibling("alpha.upgrade")))
    }


    @Test
    fun `upgrade of a running project fails cleanly and rolls back (Windows lock probe)`() {
        if (!isWindows()) {
            return
        }

        val home = createdProjectHome("alpha", "0.29.1")

        // Hold main.jar open the way a running child JVM would (its classpath JarFile handle). java.io's
        //  FileInputStream opens without FILE_SHARE_DELETE on Windows, so the backup rename fails with a
        //  sharing violation — unlike NIO Files.newInputStream, which shares delete and would NOT reproduce
        //  the lock. (Production sees the same lock from the child's URLClassLoader-held jar.)
        java.io.FileInputStream(home.resolve("main.jar").toFile()).use {
            assertFailsWith<IllegalStateException> {
                creator().upgrade(home, archetype("0.30.0"))
            }
        }

        // rolled back to the original version, no residue
        assertEquals("main-0.29.1", Files.readString(home.resolve("main.jar")))
        assertTrue(Files.exists(home.resolve("dependencies/dep-0.29.1.jar")))
        assertFalse(Files.exists(home.resolve("dependencies/dep-0.30.0.jar")))
        assertFalse(Files.exists(home.resolve("main.jar.old")))
        assertFalse(Files.exists(home.resolve("dependencies.old")))
        assertFalse(Files.exists(home.resolveSibling("alpha.upgrade")))
    }


    @Test
    fun `leftover backups and staging from a failed attempt are cleaned on the next upgrade`() {
        val home = createdProjectHome("alpha", "0.29.1")

        // Simulate residue of a crashed prior attempt.
        Files.writeString(home.resolve("main.jar.old"), "stale backup")
        Files.createDirectories(home.resolve("dependencies.old"))
        Files.writeString(home.resolve("dependencies.old/stale.jar"), "stale")
        Files.createDirectories(home.resolveSibling("alpha.upgrade"))
        Files.writeString(home.resolveSibling("alpha.upgrade/leftover"), "stale staging")

        creator().upgrade(home, archetype("0.30.0"))

        assertEquals("main-0.30.0", Files.readString(home.resolve("main.jar")))
        assertTrue(Files.exists(home.resolve("dependencies/dep-0.30.0.jar")))
        assertFalse(Files.exists(home.resolve("main.jar.old")))
        assertFalse(Files.exists(home.resolve("dependencies.old")))
        assertFalse(Files.exists(home.resolveSibling("alpha.upgrade")))
    }


    @Test
    fun `upgrade of a non-project directory is rejected`() {
        val notAProject = Files.createDirectories(projectHome.resolve("empty"))

        assertFailsWith<IllegalArgumentException> {
            creator().upgrade(notAProject, archetype("0.30.0"))
        }
    }
}
