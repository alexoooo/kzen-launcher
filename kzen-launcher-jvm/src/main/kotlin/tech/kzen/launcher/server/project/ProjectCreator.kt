package tech.kzen.launcher.server.project

//import org.springframework.stereotype.Component
import com.google.common.io.ByteStreams
import com.google.common.io.MoreFiles
import com.google.common.io.RecursiveDeleteOption
import org.slf4j.LoggerFactory
import tech.kzen.launcher.server.archetype.ArchetypeInfo
import tech.kzen.launcher.server.archetype.ArchetypeRepo
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


//@Component
class ProjectCreator(
    val archetypeRepo: ArchetypeRepo,
    private val projectHome: Path
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ProjectCreator::class.java)!!

        private const val mainJarName = "main.jar"
        private const val dependenciesDirName = "dependencies"

        // A project is built in a sibling staging dir and atomically swapped in, so a failed create
        //  never leaves a partial project that the check(!exists) below would then refuse to retry.
        private const val stagingSuffix = ".staging"

        // An upgrade extracts into a sibling of the project home, then swaps main.jar + dependencies/ in
        //  through .old backups so a running-project lock (Windows) fails cleanly with everything rolled back.
        private const val upgradeSuffix = ".upgrade"
        private const val backupSuffix = ".old"


        // https://askubuntu.com/questions/638796/what-is-meaning-of-755-permissions-in-samba-share
        private val executablePermissions = setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,

                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.GROUP_READ,

                PosixFilePermission.OTHERS_EXECUTE,
                PosixFilePermission.OTHERS_READ)


        private val isPosix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix")
    }


    fun create(
            name: String, archetypeName: String
    ): Path {
        val home = projectHome.resolve(name)

        check(!Files.exists(home)) {"already exists: $home"}

        val archetypeInfo = archetypeRepo.get(archetypeName)

        val staging = home.resolveSibling(home.fileName.toString() + stagingSuffix)
        if (Files.exists(staging)) {
            MoreFiles.deleteRecursively(staging, RecursiveDeleteOption.ALLOW_INSECURE)
        }
        Files.createDirectories(staging)

        extractGradle(staging, archetypeInfo.location)

        check(Files.exists(staging.resolve(mainJarName))) {
            "archetype missing $mainJarName: ${archetypeInfo.location}"
        }

        swapIntoPlace(staging, home)

        return home
    }


    // Replaces main.jar + dependencies/ in an existing project home from a selected archetype, preserving
    //  everything else (notation/, work/, logs/, user files, and the OLD archetype's seed docs — the new
    //  archetype's seed is deliberately NOT imported). Crash-safe and lock-probing: on Windows a running
    //  project holds main.jar (and its dep jars) open without FILE_SHARE_DELETE, so the rename in step 2 fails
    //  and the whole operation rolls back to a byte-identical home and throws IllegalStateException (→ 409).
    //  POSIX has no such lock, so an upgrade of a running project there succeeds silently (old inodes keep
    //  serving) — the same accepted residual as delete; the UI filter is the real guard.
    fun upgrade(home: Path, archetypeInfo: ArchetypeInfo) {
        require(Files.exists(home)) { "not a project home: $home" }
        require(Files.exists(home.resolve(mainJarName))) { "not a project home (no $mainJarName): $home" }

        val staging = home.resolveSibling(home.fileName.toString() + upgradeSuffix)
        if (Files.exists(staging)) {
            MoreFiles.deleteRecursively(staging, RecursiveDeleteOption.ALLOW_INSECURE)
        }
        Files.createDirectories(staging)

        try {
            extractGradle(staging, archetypeInfo.location)
            check(Files.exists(staging.resolve(mainJarName))) {
                "archetype missing $mainJarName: ${archetypeInfo.location}"
            }

            swapUpgradeIntoPlace(home, staging)
        }
        finally {
            deleteQuietly(staging)
        }
    }


    // Backup-then-swap with rollback. The order (jar first, then the dependencies directory) makes the
    //  running-project lock the enforcement mechanism: the jar rename is the probe, and nothing in home is
    //  touched until it succeeds, so a locked jar leaves home untouched.
    private fun swapUpgradeIntoPlace(home: Path, staging: Path) {
        val mainJar = home.resolve(mainJarName)
        val mainJarBackup = home.resolve(mainJarName + backupSuffix)
        val dependencies = home.resolve(dependenciesDirName)
        val dependenciesBackup = home.resolve(dependenciesDirName + backupSuffix)

        // Residue of a prior failed attempt would block the backup renames — clear it first (best-effort; a
        //  locked leftover means a running project, so surface it as the same 409).
        try {
            deleteRecursivelyIfExists(mainJarBackup)
            deleteRecursivelyIfExists(dependenciesBackup)
        }
        catch (e: IOException) {
            throw IllegalStateException(
                "Could not clear a previous upgrade's backup (is the project still running?): $home", e)
        }

        // Lock probe: on Windows this fails while the project is running (child JVM holds the jar).
        try {
            Files.move(mainJar, mainJarBackup)
        }
        catch (e: IOException) {
            throw IllegalStateException(
                "Could not replace $mainJarName (is the project still running?): $home", e)
        }

        val hadDependencies = Files.exists(dependencies)
        if (hadDependencies) {
            try {
                Files.move(dependencies, dependenciesBackup)
            }
            catch (e: IOException) {
                // Roll back the jar so home is byte-identical again.
                Files.move(mainJarBackup, mainJar)
                throw IllegalStateException(
                    "Could not replace $dependenciesDirName (is the project still running?): $home", e)
            }
        }

        try {
            Files.move(staging.resolve(mainJarName), mainJar)
            val stagingDependencies = staging.resolve(dependenciesDirName)
            if (Files.exists(stagingDependencies)) {
                Files.move(stagingDependencies, dependencies)
            }
        }
        catch (e: IOException) {
            // Roll both backups back and rethrow — home returns to its pre-upgrade state.
            deleteRecursivelyIfExists(mainJar)
            deleteRecursivelyIfExists(dependencies)
            Files.move(mainJarBackup, mainJar)
            if (hadDependencies) {
                Files.move(dependenciesBackup, dependencies)
            }
            throw IllegalStateException("Could not install the upgraded jar: $home", e)
        }

        // Best-effort cleanup of the backups; a residual doesn't fail the (already-successful) upgrade.
        deleteQuietly(mainJarBackup)
        deleteQuietly(dependenciesBackup)
    }


    private fun deleteRecursivelyIfExists(path: Path) {
        if (Files.exists(path)) {
            MoreFiles.deleteRecursively(path, RecursiveDeleteOption.ALLOW_INSECURE)
        }
    }


    // Prune/cleanup deletes are best-effort: a residual backup or staging dir must not fail a successful
    //  upgrade, and on Windows a lingering lock can block it (surfaced already by the swap probe above).
    private fun deleteQuietly(path: Path) {
        try {
            deleteRecursivelyIfExists(path)
        }
        catch (e: Exception) {
            logger.warn("unable to delete: {} - {}", path, e.toString())
        }
    }


    private fun swapIntoPlace(staging: Path, home: Path) {
        try {
            Files.move(staging, home, StandardCopyOption.ATOMIC_MOVE)
        }
        catch (e: AtomicMoveNotSupportedException) {
            // staging and home on different stores — a plain move copies then deletes.
            logger.info("atomic move unsupported ({}), copying across stores: {} -> {}", e.message, staging, home)
            Files.move(staging, home)
        }
    }


    private fun extractGradle(path: Path, zipLocation: Path) {
        Files.newInputStream(zipLocation).use { input ->
            unzip(input, path)
        }

        val gradleWrapper = path.resolve("gradlew")

        if (isPosix) {
            if (Files.exists(gradleWrapper)) {
                Files.setPosixFilePermissions(gradleWrapper, executablePermissions)
            }
        }
    }



    private fun unzip(zipFile: InputStream, destDirectory: Path) {
        ZipInputStream(zipFile).use { zipIn ->
            while (true) {
                val entry: ZipEntry =
                        zipIn.nextEntry
                        ?: break

                val filePath = resolveEntry(destDirectory, entry)

                if (entry.isDirectory) {
                    Files.createDirectories(filePath)
                }
                else {
                    Files.createDirectories(filePath.parent)
                    Files.newOutputStream(filePath).use {
                        ByteStreams.copy(zipIn, it)
                    }
                }
                zipIn.closeEntry()
            }
        }
    }


    // Guards against zip-slip: a crafted entry name (e.g. ../) must not resolve outside the target dir.
    private fun resolveEntry(destDirectory: Path, entry: ZipEntry): Path {
        val filePath = destDirectory.resolve(entry.name)
        val destDirPath = destDirectory.toFile().canonicalPath
        val entryPath = filePath.toFile().canonicalPath
        if (entryPath != destDirPath && !entryPath.startsWith(destDirPath + File.separator)) {
            throw IOException("Entry is outside of the target dir: ${entry.name}")
        }
        return filePath
    }
}
