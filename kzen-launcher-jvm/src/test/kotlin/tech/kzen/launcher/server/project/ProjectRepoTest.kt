package tech.kzen.launcher.server.project

import com.google.common.io.MoreFiles
import com.google.common.io.RecursiveDeleteOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class ProjectRepoTest {
    //-----------------------------------------------------------------------------------------------------------------
    private val tempDirs = mutableListOf<Path>()

    private val projectHome = tempDir()
    private val metadata = projectHome.resolve("kzen-projects.yaml")


    @AfterTest
    fun tearDown() {
        for (dir in tempDirs) {
            MoreFiles.deleteRecursively(dir, RecursiveDeleteOption.ALLOW_INSECURE)
        }
    }


    private fun tempDir(): Path {
        val dir = Files.createTempDirectory("project-repo-test")
        tempDirs.add(dir)
        return dir
    }


    private fun repo(): ProjectRepo {
        return ProjectRepo(projectHome)
    }


    private fun projectDir(name: String): Path {
        return Files.createDirectories(projectHome.resolve(name))
    }


    private fun assertNoTempResidue() {
        assertFalse(Files.exists(projectHome.resolve("kzen-projects.yaml.tmp")))
    }


    //-----------------------------------------------------------------------------------------------------------------
    @Test
    fun `entries survive a relaunch`() {
        val first = repo()
        first.add("alpha", projectHome.resolve("alpha"))
        first.add("beta", projectHome.resolve("beta"))
        first.changeArguments("beta", "-Xmx2g")

        val relaunched = repo()

        assertEquals(setOf("alpha", "beta"), relaunched.all().keys)
        assertEquals("-Xmx2g", relaunched.get("beta").jvmArguments)
        assertEquals("", relaunched.get("alpha").jvmArguments)
        assertTrue(Files.exists(metadata))
        assertNoTempResidue()
    }


    @Test
    fun `hand-written registry parses and mutates in place`() {
        val handWritten = projectHome.resolve("handmade").toAbsolutePath().normalize().toString()
        Files.writeString(metadata,
            "handmade:\n" +
            "  home: \"${handWritten.replace("\\", "\\\\")}\"\n" +
            "  args: \"-Xmx1g\"\n")

        val repo = repo()
        assertEquals("-Xmx1g", repo.get("handmade").jvmArguments)

        repo.add("added", projectHome.resolve("added"))

        val relaunched = repo()
        assertEquals(setOf("handmade", "added"), relaunched.all().keys)
        assertEquals("-Xmx1g", relaunched.get("handmade").jvmArguments)
    }


    @Test
    fun `edits while running are invisible until relaunch`() {
        val repo = repo()
        repo.add("alpha", projectHome.resolve("alpha"))

        Files.writeString(metadata, "external:\n  home: \"/somewhere/external\"\n  args: \"\"\n")

        assertEquals(setOf("alpha"), repo.all().keys)
        assertEquals(setOf("external"), repo().all().keys)
    }


    @Test
    fun `missing registry starts empty and is created on first mutation`() {
        val repo = repo()
        assertTrue(repo.all().isEmpty())
        assertFalse(Files.exists(metadata))

        repo.add("alpha", projectHome.resolve("alpha"))

        assertTrue(Files.exists(metadata))
        assertEquals(setOf("alpha"), repo().all().keys)
    }


    @Test
    fun `unparseable registry fails construction`() {
        Files.writeString(metadata, "not a key-value map")

        assertFailsWith<IllegalStateException> { repo() }
    }


    @Test
    fun `duplicate add is rejected`() {
        val repo = repo()
        repo.add("alpha", projectHome.resolve("alpha"))

        assertFailsWith<IllegalArgumentException> { repo.add("alpha", projectHome.resolve("alpha")) }
    }


    //-----------------------------------------------------------------------------------------------------------------
    @Test
    fun `archetype and version round-trip through a relaunch`() {
        val first = repo()
        first.add("alpha", projectHome.resolve("alpha"), "kzen-project", "0.30.0-SNAPSHOT")
        first.add("beta", projectHome.resolve("beta"))

        val relaunched = repo()
        assertEquals("kzen-project", relaunched.get("alpha").archetype)
        assertEquals("0.30.0-SNAPSHOT", relaunched.get("alpha").version)
        // add() without archetype/version records unknown
        assertEquals(ProjectRepo.unknownValue, relaunched.get("beta").archetype)
        assertEquals(ProjectRepo.unknownValue, relaunched.get("beta").version)
    }


    @Test
    fun `legacy yaml without archetype fields binds to unknown`() {
        val home = projectHome.resolve("legacy").toAbsolutePath().normalize().toString()
        Files.writeString(metadata,
            "legacy:\n" +
            "  home: \"${home.replace("\\", "\\\\")}\"\n" +
            "  args: \"\"\n")

        val repo = repo()
        assertEquals(ProjectRepo.unknownValue, repo.get("legacy").archetype)
        assertEquals(ProjectRepo.unknownValue, repo.get("legacy").version)
    }


    @Test
    fun `recordArchetype persists the new archetype and version`() {
        val repo = repo()
        repo.add("alpha", projectHome.resolve("alpha"), "kzen-project", "0.29.1")

        repo.recordArchetype("alpha", "kzen-project", "0.30.0-SNAPSHOT")

        assertEquals("0.30.0-SNAPSHOT", repo.get("alpha").version)
        assertEquals("0.30.0-SNAPSHOT", repo().get("alpha").version)
    }


    @Test
    fun `recordArchetype on a missing project is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            repo().recordArchetype("nope", "kzen-project", "0.30.0")
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    @Test
    fun `concurrent mutations neither lose an update nor corrupt the file`() {
        val addCount = 40
        val renameCount = 10
        val removeCount = 10
        val argsCount = 10

        val seeded = repo()
        for (i in 0 until renameCount) {
            projectDir("rename-$i")
            seeded.add("rename-$i", projectHome.resolve("rename-$i"))
        }
        for (i in 0 until removeCount) {
            seeded.add("remove-$i", projectHome.resolve("remove-$i"))
        }
        for (i in 0 until argsCount) {
            seeded.add("args-$i", projectHome.resolve("args-$i"))
        }

        val repo = repo()
        runBlocking(Dispatchers.Default) {
            val jobs = buildList {
                for (i in 0 until addCount) {
                    add(launch { repo.add("add-$i", projectHome.resolve("add-$i")) })
                }
                for (i in 0 until renameCount) {
                    add(launch { repo.rename("rename-$i", "renamed-$i") })
                }
                for (i in 0 until removeCount) {
                    add(launch { repo.remove("remove-$i") })
                }
                for (i in 0 until argsCount) {
                    add(launch { repo.changeArguments("args-$i", "-Xmx${i}g") })
                }
            }
            jobs.joinAll()
        }

        val expected =
            (0 until addCount).map { "add-$it" }.toSet() +
            (0 until renameCount).map { "renamed-$it" }.toSet() +
            (0 until argsCount).map { "args-$it" }.toSet()

        for (registry in listOf(repo, repo())) {
            assertEquals(expected, registry.all().keys)
            for (i in 0 until argsCount) {
                assertEquals("-Xmx${i}g", registry.get("args-$i").jvmArguments)
            }
            assertEquals(
                projectHome.resolve("renamed-0").toAbsolutePath().normalize(),
                registry.get("renamed-0").home.toAbsolutePath().normalize())
        }

        assertNoTempResidue()
    }
}
