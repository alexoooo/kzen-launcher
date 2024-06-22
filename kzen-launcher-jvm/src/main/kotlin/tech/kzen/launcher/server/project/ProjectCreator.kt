package tech.kzen.launcher.server.project

//import org.springframework.stereotype.Component
import com.google.common.io.ByteStreams
import com.google.common.io.MoreFiles
import org.slf4j.LoggerFactory
import tech.kzen.launcher.server.archetype.ArchetypeRepo
import tech.kzen.launcher.server.environment.LauncherEnvironment
import java.io.InputStream
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


//@Component
class ProjectCreator(
    val archetypeRepo: ArchetypeRepo
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ProjectCreator::class.java)!!


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

        check(! Files.exists(home)) {"already exists: $home"}

        val archetypeInfo = archetypeRepo.get(archetypeName)

        @Suppress("MoveVariableDeclarationIntoWhen")
        val artifactExtension = MoreFiles.getFileExtension(archetypeInfo.location)

        when (artifactExtension) {
            "zip" -> {
                extractGradle(home, archetypeInfo.location)
            }

            "jar" -> {
                Files.createDirectories(home)
                Files.copy(archetypeInfo.location, home.resolve("main.jar"))
            }

            else ->
                    throw IllegalStateException("Unknown archetype: ${archetypeInfo.location}")
        }

        return home
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
        val zipIn = ZipInputStream(zipFile)

        while (true) {
            val entry: ZipEntry =
                    zipIn.nextEntry
                    ?: break

            val filePath = destDirectory.resolve(entry.name)

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