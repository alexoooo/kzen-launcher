package tech.kzen.launcher.server.api

import com.google.common.io.MoreFiles
import com.google.common.io.Resources
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import tech.kzen.launcher.server.service.ProjectService
import reactor.core.publisher.Mono
import tech.kzen.launcher.common.CommonApi
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


@Component
class RestHandler(
        val projectService: ProjectService
) {
    //-----------------------------------------------------------------------------------------------------------------
    companion object {
        private val classPathRoots = listOf(
                URI("classpath:/public/"))

        private val resourceDirectories = listOf<Path>(
                // IntelliJ and typical commandline working dir is project root
                Paths.get("kzen-launcher-jvm/src/main/resources/public/"),
                Paths.get("kzen-launcher-js/build/dist/"),

                // Eclipse default active working directory is the module
                Paths.get("src/main/resources/public/"),
                Paths.get("../kzen-launcher-js/build/dist/"))

        private val allowedExtensions = listOf(
                "html",
                "js",
                "css",
                "ico")

        private const val automationProject = "automation"
        private const val automationVersion = "0.0.2"
        private val automationUrl = URI("https://raw.githubusercontent.com/alexoooo/kzen-repo/master/artifacts/" +
                "tech/kzen/project/kzen-project/$automationVersion/kzen-project-$automationVersion.zip")
    }


    //-----------------------------------------------------------------------------------------------------------------
    fun archetypes(serverRequest: ServerRequest): Mono<ServerResponse> =
            ServerResponse.ok().body(Mono.just(
                    "{" +
                            "\"$automationProject\":" +
                            "\"$automationUrl\"" +
                    "}"))


    fun create(serverRequest: ServerRequest): Mono<ServerResponse> {
//        val path = serverRequest.path()
//        val projectName = path.substringAfterLast('/')

        val projectName = serverRequest.queryParam(CommonApi.createProjectName).get()

        val archetypeName = serverRequest.queryParam(CommonApi.createProjectType).get()
        if (archetypeName != automationProject) {
            return ServerResponse.badRequest().body(Mono.just("unknown project type: $archetypeName"))
        }

        projectService.create(projectName, automationUrl)

        return ServerResponse.ok().build()
    }



    //-----------------------------------------------------------------------------------------------------------------
    // TODO: is this secure?
    fun resource(serverRequest: ServerRequest): Mono<ServerResponse> {
        val excludingInitialSlash = serverRequest.path().substring(1)

        val resolvedPath =
                if (excludingInitialSlash == "") {
                    "index.html"
                }
                else {
                    excludingInitialSlash
                }

        val path = Paths.get(resolvedPath).normalize()

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
            catch (ignored: Exception) {}
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