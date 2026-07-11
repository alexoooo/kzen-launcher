package tech.kzen.launcher.server.service

import org.slf4j.LoggerFactory
import java.io.BufferedOutputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path


// Downloads executable artifacts (archetype zips/jars), so TLS certificates are validated with
//  the JVM's default trust store — corporate-MITM environments can supply their own via
//  -Djavax.net.ssl.trustStore (see README).
class DownloadService {
    //-----------------------------------------------------------------------------------------------------------------
    companion object {
        private val logger = LoggerFactory.getLogger(DownloadService::class.java)!!
    }


    //-----------------------------------------------------------------------------------------------------------------
    fun download(location: URI, destination: Path) {
        Files.createDirectories(destination.parent)

        logger.info("downloading: {}", location)

        val bytes = BufferedOutputStream(
            Files.newOutputStream(destination)
        ).use { output ->
            location
                .toURL()
                .openStream()
                .use { input -> input.copyTo(output) }
        }

        logger.info("download complete: {}", bytes)
    }
}
