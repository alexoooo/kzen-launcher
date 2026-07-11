package tech.kzen.launcher.server.project

//import org.springframework.stereotype.Component
import com.google.common.io.ByteStreams
import com.google.common.io.MoreFiles
import com.google.common.io.RecursiveDeleteOption
import org.slf4j.LoggerFactory
import tech.kzen.launcher.server.archetype.ArchetypeRepo
import tech.kzen.launcher.server.environment.LauncherEnvironment
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
    val archetypeRepo: ArchetypeRepo
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ProjectCreator::class.java)!!

        private const val mainJarName = "main.jar"

        // A project is built in a sibling staging dir and atomically swapped in, so a failed create
        //  never leaves a partial project that the check(!exists) below would then refuse to retry.
        private const val stagingSuffix = ".staging"


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
        val home = LauncherEnvironment.projectHome.resolve(name)

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
