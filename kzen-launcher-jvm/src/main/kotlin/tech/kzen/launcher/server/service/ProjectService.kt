package tech.kzen.launcher.server.service

import com.google.common.io.ByteStreams
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.*
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermission
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry


@Component
class ProjectService {
    companion object {
        private val logger = LoggerFactory.getLogger(ProjectService::class.java)!!

        private val projectHome = Paths.get("proj")!!


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
            name: String, download: URI
    ) {
        val path = projectHome.resolve(name)

        check(! Files.exists(path), {"already exists: $path"})

        logger.info("downloading: {}", download)

        val downloadBytes = download
                .toURL()
                .openStream()
                .use { ByteStreams.toByteArray(it) }

        logger.info("download complete: {}", downloadBytes.size)

        unzip(ByteArrayInputStream(downloadBytes), path)

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