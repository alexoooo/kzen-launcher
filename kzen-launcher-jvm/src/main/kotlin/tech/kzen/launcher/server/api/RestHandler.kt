package tech.kzen.launcher.server.api

import com.google.common.collect.ImmutableMap
import com.google.common.io.Resources
import io.ktor.http.*
import tech.kzen.launcher.common.api.CommonRestApi
import tech.kzen.launcher.server.archetype.ArchetypeInfo
import tech.kzen.launcher.server.project.ProjectCreator
import tech.kzen.launcher.server.archetype.ArchetypeRepo
import tech.kzen.launcher.server.project.ProjectRepo
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


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
                Paths.get("kzen-launcher-js/build/distributions/"),

                // Eclipse default active working directory is the module
                Paths.get("src/main/resources/public/"),
                Paths.get("../kzen-launcher-js/build/distributions/"))


        private const val cssExtension = "css"

        private val allowedExtensions = listOf(
                "html",
                "js",
                cssExtension,
                "ico",
                "png")

//        private val cssMediaType = MediaType.valueOf("text/css")
    }


    //-----------------------------------------------------------------------------------------------------------------
    fun runningProjectsDummy(): List<Any> {
        return listOf()
    }


    //-----------------------------------------------------------------------------------------------------------------
    fun listArchetypes(): ImmutableMap<String, ArchetypeInfo> {
        return archetypeRepo.all()
    }


    fun listProjects(): List<Map<String, Any>> {
        return projectRepo
            .all()
            .map {
                val path = it.value.home
                val normalized = path.toString().replace('\\', '/')
                val exists = Files.exists(path)

                mapOf(
                    CommonRestApi.projectName to it.key,
                    CommonRestApi.projectPath to normalized,
                    CommonRestApi.projectJvmArgs to it.value.jvmArguments,
                    CommonRestApi.projectExists to exists)
            }
    }


    fun createProject(parameters: Parameters) {
        val projectName = parameters.getParam(CommonRestApi.projectName)
        val archetypeName = parameters.getParam(CommonRestApi.createProjectType)

        val projectHome = projectCreator.create(projectName, archetypeName)
        projectRepo.add(projectName, projectHome)
    }


    fun importProject(parameters: Parameters) {
        val projectHome = parameters.getParam(CommonRestApi.projectPath, Paths::get)
        val projectName = projectHome.fileName.toString()

        projectRepo.add(projectName, projectHome)
    }


    fun removeProject(parameters: Parameters) {
        val projectName = parameters.getParam(CommonRestApi.projectName)
        projectRepo.remove(projectName)
    }


    fun deleteProject(parameters: Parameters) {
        val projectName = parameters.getParam(CommonRestApi.projectName)
        projectRepo.delete(projectName)
    }


    fun renameProject(parameters: Parameters) {
        val projectName = parameters.getParam(CommonRestApi.projectName)
        val newName = parameters.getParam(CommonRestApi.projectNewName)

        projectRepo.rename(projectName, newName)
    }


    fun jvmArgumentsProject(parameters: Parameters) {
        val projectName = parameters.getParam(CommonRestApi.projectName)
        val jvmArguments = parameters.getParam(CommonRestApi.projectJvmArgs)

        projectRepo.changeArguments(projectName, jvmArguments)
    }


    //-----------------------------------------------------------------------------------------------------------------
    // TODO: is this secure?
//    fun resource(serverRequest: ServerRequest): Mono<ServerResponse> {
//        val excludingInitialSlash = serverRequest.path().substring(1)
//
//        val resolvedPath =
//            if (excludingInitialSlash == "") {
//                "index.html"
//            }
//            else {
//                excludingInitialSlash
//            }
//
//        val path = Paths.get(resolvedPath).normalize()
//        val extension = MoreFiles.getFileExtension(path)
//
//        if (! isResourceAllowed(path, extension)) {
//            return ServerResponse.badRequest().build()
//        }
//
//        val bytes: ByteArray = readResource(path)
//            ?: return ServerResponse.notFound().build()
//
//        val builder = ServerResponse.ok()
//
//        val responseType: MediaType? = responseType(extension)
//        if (responseType !== null) {
//            builder.contentType(responseType)
//        }
//
//        return builder
//                .body(Mono.just(bytes))
//    }
//
//
//    private fun responseType(extension: String): MediaType? {
//        return when (extension) {
//            cssExtension -> cssMediaType
//
//            else -> null
//        }
//    }


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




    //-----------------------------------------------------------------------------------------------------------------
    private fun Parameters.getParam(
        parameterName: String
    ): String {
        return getParam(parameterName) { it }
    }

    private fun <T> Parameters.getParam(
        parameterName: String,
        parser: (String) -> T
    ): T {
        val queryParamValues: List<String>? = getAll(parameterName)
        require(! queryParamValues.isNullOrEmpty()) { "'$parameterName' required" }
        require(queryParamValues.size == 1) { "Single '$parameterName' expected: $queryParamValues" }
        return parser(queryParamValues.single())
    }


    private fun <T> Parameters.getParamList(
        parameterName: String,
        parser: (String) -> T
    ): List<T> {
        val queryParamValues: List<String> = getAll(parameterName)
            ?: return listOf()
        return queryParamValues.map(parser)
    }


    private fun <T> Parameters.getParamOrNull(
        parameterName: String,
        parser: (String) -> T
    ): T? {
        val queryParamValues: List<String> = getAll(parameterName)
            ?: return null

        require(queryParamValues.isNotEmpty()) { "'$parameterName' required" }
        require(queryParamValues.size == 1) { "Single '$parameterName' expected: $queryParamValues" }

        return parser(queryParamValues.single())
    }
}