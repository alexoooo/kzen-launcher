package tech.kzen.launcher.server.project

import com.google.common.io.ByteStreams
import com.google.common.io.MoreFiles
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tech.kzen.launcher.server.archetype.ArchetypeRepo
import tech.kzen.launcher.server.environment.LauncherEnvironment
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry


@Component
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
    }


    fun create(
            name: String, archetypeName: String
    ): Path {
        val home = LauncherEnvironment.projectHome.resolve(name)

        check(! Files.exists(home), {"already exists: $home"})

        val archetypeInfo = archetypeRepo.get(archetypeName)
        val archetypeBytes = Files.readAllBytes(archetypeInfo.artifact)

        val artifactExtension = MoreFiles.getFileExtension(archetypeInfo.artifact)

        when (artifactExtension) {
            "zip" -> {
                extractGradle(home, archetypeBytes)
            }

            "jar" -> {
                Files.createDirectories(home)
                Files.write(home.resolve("main.jar"), archetypeBytes)
            }

            else ->
                    throw IllegalStateException("Unknown archetype: ${archetypeInfo.artifact}")
        }

        return home
    }


    private fun extractGradle(path: Path, archetypeBytes: ByteArray) {
        unzip(ByteArrayInputStream(archetypeBytes), path)

        val gradleWrapper = path.resolve("gradlew")

        if (Files.exists(gradleWrapper)) {
            Files.setPosixFilePermissions(gradleWrapper, executablePermissions)
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


//    /**
//     * Extracts a zip entry (file entry)
//     * @param zipIn
//     * @param filePath
//     * @throws IOException
//     */
//    @Throws(IOException::class)
//    private fun extractFile(zipIn: ZipInputStream, filePath: String) {
//        val bos = BufferedOutputStream(FileOutputStream(filePath))
//        val bytesIn = ByteArray(BUFFER_SIZE)
//        var read = 0
//        while ((read = zipIn.read(bytesIn)) != -1) {
//            bos.write(bytesIn, 0, read)
//        }
//        bos.close()
//    }
}