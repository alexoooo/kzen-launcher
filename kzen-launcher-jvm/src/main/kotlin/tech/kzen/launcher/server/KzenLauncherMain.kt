package tech.kzen.launcher.server

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tech.kzen.launcher.common.api.CommonRestApi
import tech.kzen.launcher.common.api.staticResourceDir
import tech.kzen.launcher.common.api.staticResourcePath
import tech.kzen.launcher.common.dto.ArchetypeDetail
import tech.kzen.launcher.server.api.RestHandler
import tech.kzen.launcher.server.archetype.ArchetypeInfo
import tech.kzen.launcher.server.archetype.ArchetypeRepo
import tech.kzen.launcher.server.backend.indexPage
import tech.kzen.launcher.server.project.ProjectCreator
import tech.kzen.launcher.server.project.ProjectRepo
import tech.kzen.launcher.server.properties.KzenProperties
import tech.kzen.launcher.server.service.DownloadService


//---------------------------------------------------------------------------------------------------------------------
fun main(args: Array<String>) {
    val context = buildContext(args)
    context.init()
    kzenLauncherMain(context)
}


//---------------------------------------------------------------------------------------------------------------------
data class KzenLauncherConfig(
    val jsModuleName: String,
    val port: Int = 80,
    val host: String = "127.0.0.1"
) {
    //-----------------------------------------------------------------------------------------------------------------
    companion object {
        private const val serverPortPrefix = "--server.port="
        private val serverPortRegex = Regex(
            Regex.escape(serverPortPrefix) + "\\d+")

        fun readPort(args: Array<String>): Int? {
            val match = args
                .lastOrNull { it.matches(serverPortRegex) }
                ?: return null

            val portText = match.substring(serverPortPrefix.length)
            return portText.toInt()
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    fun jsFileName(): String {
        return "$jsModuleName.js"
    }

    fun jsResourcePath(): String {
        return "$staticResourcePath/${jsFileName()}"
    }
}


data class KzenLauncherContext(
    val config: KzenLauncherConfig,
    val restApi: RestHandler,
    val downloadService: DownloadService,
    val archetypeRepo: ArchetypeRepo,
) {
    fun init() {
        downloadService.trustBadCertificate()
        archetypeRepo.init()
    }
}



//---------------------------------------------------------------------------------------------------------------------
const val kzenLauncherJsModuleName = "kzen-launcher-js"
//const val jsResourcePath = "$staticResourcePath/$jsFileName"

private const val indexFileName = "index.html"
private const val indexFilePath = "/$indexFileName"


//---------------------------------------------------------------------------------------------------------------------
fun buildContext(args: Array<String>): KzenLauncherContext {
    val kzenProperties = KzenProperties()
    val projectArchetype = KzenProperties.Archetype()
    projectArchetype.name = "KzenProjectJar-0.27.0"
    projectArchetype.title = "Automation and Reporting"
    projectArchetype.description = "Visually control a browser and more - v0.27.0"
    projectArchetype.url = "file:///C:/Users/ostro/IdeaProjects/kzen-project/kzen-project-jvm/build/libs/kzen-project-jvm-0.27.0.zip"
//    projectArchetype.url = "https://github.com/alexoooo/kzen-project/releases/download/v0.26.0/kzen-project-0.26.0.zip"
    kzenProperties.archetypes.add(projectArchetype)

    val downloadService = DownloadService()
    val archetypeRepo = ArchetypeRepo(downloadService, kzenProperties)
    val projectRepo = ProjectRepo()
    val projectCreator = ProjectCreator(archetypeRepo)
    val restHandler = RestHandler(archetypeRepo, projectRepo, projectCreator)
//    val serverRestApi = ServerRestApi(restHandler)

    val port = KzenLauncherConfig.readPort(args) ?: 8080

    val config = KzenLauncherConfig(
        kzenLauncherJsModuleName,
        port = port
    )

    return KzenLauncherContext(
        config, restHandler, downloadService, archetypeRepo)
}


//---------------------------------------------------------------------------------------------------------------------
fun kzenLauncherMain(context: KzenLauncherContext) {
    embeddedServer(
        Netty,
        port = context.config.port,
        host = context.config.host
    ) {
        ktorMain(context)
    }.start(wait = true)
}


fun Application.ktorMain(
    context: KzenLauncherContext
) {
    install(ContentNegotiation) {
        jackson()
    }

    routing {
        routeRequests(context)
    }
}


//---------------------------------------------------------------------------------------------------------------------
private fun Routing.routeRequests(
    context: KzenLauncherContext
) {
    get("/") {
        call.respondRedirect(indexFileName)
    }
    get(indexFilePath) {
        call.respondHtml(HttpStatusCode.OK) {
            indexPage(context.config)
        }
    }

    staticResources(staticResourcePath, staticResourceDir)

    routeRest(context.restApi)
}


private fun Routing.routeRest(
    restHandler: RestHandler
) {
    get(CommonRestApi.listArchetypes) {
        val response = restHandler.listArchetypes()
        call.respond(response.entries.map { e -> ArchetypeDetail(
            e.key,
            e.value.title,
            e.value.description,
            e.value.location.toAbsolutePath().normalize().toString())
        })
    }

    get(CommonRestApi.listProjects) {
        val response = restHandler.listProjects()
        call.respond(response)
    }
    get(CommonRestApi.createProject) {
        restHandler.createProject(call.parameters)
        call.response.status(HttpStatusCode.OK)
    }
    get(CommonRestApi.importProject) {
        restHandler.importProject(call.parameters)
        call.response.status(HttpStatusCode.OK)
    }
    get(CommonRestApi.removeProject) {
        restHandler.removeProject(call.parameters)
        call.response.status(HttpStatusCode.OK)
    }
    get(CommonRestApi.deleteProject) {
        restHandler.deleteProject(call.parameters)
        call.response.status(HttpStatusCode.OK)
    }
    get(CommonRestApi.renameProject) {
        restHandler.renameProject(call.parameters)
        call.response.status(HttpStatusCode.OK)
    }
    get(CommonRestApi.jvmArgumentsProject) {
        restHandler.jvmArgumentsProject(call.parameters)
        call.response.status(HttpStatusCode.OK)
    }

    // Used for inline testing
    get("/shell/project") {
        val response = restHandler.runningProjectsDummy()
        call.respond(response)
    }
}
