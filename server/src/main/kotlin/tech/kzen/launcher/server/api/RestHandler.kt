package tech.kzen.launcher.server.api

import com.google.common.io.MoreFiles
import com.google.common.io.Resources
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono
import tech.kzen.launcher.common.getAnswer
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


@Component
class CounterHandler {
    //-----------------------------------------------------------------------------------------------------------------
    companion object {
        val classPathRoots = listOf(
                URI("classpath:/public/"))

        val resourceDirectories = listOf<Path>(
                // IntelliJ and typical commandline working dir is project root
                Paths.get("server/src/main/resources/public/"),
                Paths.get("client/build/dist/"),

                // Eclipse default active working directory is the module
                Paths.get("src/main/resources/public/"),
                Paths.get("../client/build/dist/"))

        val allowedExtensions = listOf(
                "html",
                "js")
    }


    //-----------------------------------------------------------------------------------------------------------------
    fun get(serverRequest: ServerRequest): Mono<ServerResponse> =
            ServerResponse
                    .ok()
                    .body(Mono.just("Foo: ${getAnswer()}"))


    //-----------------------------------------------------------------------------------------------------------------
    // TODO: is this secure?
    fun resource(serverRequest: ServerRequest): Mono<ServerResponse> {
        val excludingInitialSlash = serverRequest.path().substring(1)
        val path = Paths.get(excludingInitialSlash).normalize()

        if (! isResourceAllowed(path)) {
            return ServerResponse
                    .badRequest()
                    .build()
        }

        val bytes: ByteArray = readResource(path)
                ?: return ServerResponse
                        .notFound()
                        .build()

        return ServerResponse
                .ok()
                .body(Mono.just(bytes))
    }


    private fun isResourceAllowed(path: Path): Boolean {
        if (path.isAbsolute) {
            return false
        }

        val extension = MoreFiles.getFileExtension(path)
        return allowedExtensions.contains(extension)
    }


    private fun readResource(relativePath: Path): ByteArray? {
        for (root in classPathRoots) {
            try {
                val resourceLocation: URI = root.resolve(relativePath.toString())
                val resourceUrl = Resources.getResource(resourceLocation.path)
                return Resources.toByteArray(resourceUrl)
            }
            catch (ignored: IOException) {}
        }

        for (root in resourceDirectories) {
            val candidate = root.resolve(relativePath)
            if (Files.exists(candidate)) {
                return Files.readAllBytes(candidate)
            }
        }

        return null
    }
}