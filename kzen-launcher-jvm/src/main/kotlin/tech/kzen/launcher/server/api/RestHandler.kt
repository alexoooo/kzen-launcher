package tech.kzen.launcher.server.api

import com.google.common.io.MoreFiles
import com.google.common.io.Resources
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import tech.kzen.launcher.server.project.ProjectCreator
import reactor.core.publisher.Mono
import tech.kzen.launcher.common.CommonApi
import tech.kzen.launcher.common.dto.ArchetypeDetail
import tech.kzen.launcher.common.util.IoUtil
import tech.kzen.launcher.server.archetype.ArchetypeRepo
import tech.kzen.launcher.server.project.ProjectRepo
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


@Component
class RestHandler(
        val archetypeRepo: ArchetypeRepo,
        val projectRepo: ProjectRepo,
        val projectCreator: ProjectCreator
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


        private const val cssExtension = "css"

        private val allowedExtensions = listOf(
                "html",
                "js",
                cssExtension,
                "ico",
                "png")

        private val cssMediaType = MediaType.valueOf("text/css")

//        private const val automationProject = "automation"
//        private const val automationVersion = "0.0.2"
//        private val automationUrl = URI("https://raw.githubusercontent.com/alexoooo/kzen-repo/master/artifacts/" +
//                "tech/kzen/project/kzen-project/$automationVersion/kzen-project-$automationVersion.zip")
    }


    //-----------------------------------------------------------------------------------------------------------------
    fun runningProjectsDummy(serverRequest: ServerRequest): Mono<ServerResponse> {
        return ServerResponse.ok().body(Mono.just("[]"))
    }


    //-----------------------------------------------------------------------------------------------------------------
    fun listArchetypes(serverRequest: ServerRequest): Mono<ServerResponse> {
        val archetypes = archetypeRepo.all()

        val archetypeDetails = archetypes
                .entries
                .map {
                    val path = it.value.location
                    val normalized = path.toString().replace('\\', '/')

                    ArchetypeDetail(
                            it.key, it.value.title, it.value.description, normalized)
                }
                .toList()

//        val json = archetypes.entries
//                .joinToString(prefix = "{", postfix = "}") {
//                    val path = it.value.location
//                    val normalized = path.toString().replace('\\', '/')
//
//                    "${IoUtil.escapeJsonString(it.key)}:${IoUtil.escapeJsonString(normalized)}"
//                }

        return ServerResponse.ok()
//                .body(Mono.just(json))
                .body(Mono.just(archetypeDetails))
    }


    fun listProjects(serverRequest: ServerRequest): Mono<ServerResponse> {
        val projects = projectRepo.all()

        val json = projects.entries
                .joinToString(prefix = "[", postfix = "]") {
                    val path = it.value.home
                    val normalized = path.toString().replace('\\', '/')

                    val exists = Files.exists(path)

                    val body = mapOf(
                            "name" to it.key,
                            "path" to normalized,
                            "exists" to exists)

                    body.entries.joinToString(prefix = "{", postfix = "}") {entry ->
                        "${IoUtil.escapeJsonString(entry.key)}: ${IoUtil.escapeJson(entry.value)}"
                    }
                }

        return ServerResponse.ok().body(Mono.just(json))
    }


    fun createProject(serverRequest: ServerRequest): Mono<ServerResponse> {
        val projectName = serverRequest.queryParam(CommonApi.projectName).get()

        val archetypeName = serverRequest.queryParam(CommonApi.createProjectType).get()

        val projectHome = projectCreator.create(projectName, archetypeName)
        projectRepo.add(projectName, projectHome)

        return ServerResponse.ok().build()
    }


    fun importProject(serverRequest: ServerRequest): Mono<ServerResponse> {
        val projectPath = serverRequest.queryParam(CommonApi.importProjectPath).get()

        val projectHome = Paths.get(projectPath)

        val projectName = projectHome.fileName.toString()

        projectRepo.add(projectName, projectHome)

        return ServerResponse.ok().build()
    }


    fun removeProject(serverRequest: ServerRequest): Mono<ServerResponse> {
        val projectName = serverRequest.queryParam(CommonApi.projectName).get()

        projectRepo.remove(projectName)

        return ServerResponse.ok().build()
    }


    fun deleteProject(serverRequest: ServerRequest): Mono<ServerResponse> {
        val projectName = serverRequest.queryParam(CommonApi.projectName).get()

        projectRepo.delete(projectName)

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
        val extension = MoreFiles.getFileExtension(path)

        if (! isResourceAllowed(path, extension)) {
            return ServerResponse
                    .badRequest()
                    .build()
        }

        val bytes: ByteArray = readResource(path)
                ?: return ServerResponse
                        .notFound()
                        .build()

        val builder = ServerResponse.ok()

        val responseType: MediaType? = responseType(extension)
        if (responseType !== null) {
            builder.contentType(responseType)
        }

        return builder
                .body(Mono.just(bytes))
    }


    private fun responseType(extension: String): MediaType? {
        return when (extension) {
            cssExtension -> cssMediaType

            else -> null
        }
    }


    private fun isResourceAllowed(path: Path, extension: String): Boolean {
        if (path.isAbsolute) {
            return false
        }

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