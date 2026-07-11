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
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties
import kotlin.system.exitProcess
import tech.kzen.launcher.common.api.CommonRestApi
import tech.kzen.launcher.common.api.staticResourceDir
import tech.kzen.launcher.common.api.staticResourcePath
import tech.kzen.launcher.common.dto.ArchetypeDetail
import tech.kzen.launcher.server.api.RestHandler
import tech.kzen.launcher.server.archetype.ArchetypeRepo
import tech.kzen.launcher.server.backend.indexPage
import tech.kzen.launcher.server.dev.ShellSimulator
import tech.kzen.launcher.server.project.ProjectCreator
import tech.kzen.launcher.server.project.ProjectRepo
import tech.kzen.launcher.server.properties.KzenProperties
import tech.kzen.launcher.server.security.SecurityGate
import tech.kzen.launcher.server.service.DownloadService


//---------------------------------------------------------------------------------------------------------------------
fun main(args: Array<String>) {
    val context = buildContext(args)
    context.init()

    if (context.config.managedLifeline) {
        startManagedLifeline()
    }
    context.config.parentPid?.let { startParentWatchdog(it) }

    kzenLauncherMain(context)
}


// Managed-child lifeline (intentionally duplicated from kzen-auto's KzenAutoMain — the launcher
//  depends on neither kzen-lib nor kzen-auto, so there is no shared home worth the coupling for
//  ~20 lines). kzen-shell keeps our stdin open as a PIPE; when it closes that stream (graceful
//  stop) or dies (the OS then closes the inherited pipe on every platform) we observe EOF, and
//  exitProcess(0) frees the port. OS-agnostic, unlike Process.destroy() which is a hookless hard
//  kill (TerminateProcess) on Windows. The launcher holds no resources needing orderly shutdown.
private fun startManagedLifeline() {
    val thread = Thread({
        try {
            System.`in`.bufferedReader().use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break  // null == EOF (pipe closed / parent gone)
                    if (line.trim() == "SHUTDOWN") {
                        break
                    }
                }
            }
        }
        catch (ignored: Throwable) {
            // A read failure (e.g. the parent vanished mid-read) is itself a death signal.
        }
        exitProcess(0)
    }, "kzen-managed-lifeline")
    thread.isDaemon = true
    thread.start()
}


// Backup reaper: self-exit if the parent process exits, even if stdin EOF was somehow never
//  delivered (e.g. a future stdin redirect, or a Windows handle-inheritance corner case).
private fun startParentWatchdog(parentPid: Long) {
    ProcessHandle.of(parentPid).ifPresent { parent ->
        parent.onExit().thenRun { exitProcess(0) }
    }
}


//---------------------------------------------------------------------------------------------------------------------
data class KzenLauncherConfig(
    val jsModuleName: String,
    val port: Int = 80,
    val host: String = "127.0.0.1",

    // Managed-child lifeline flags (set by kzen-shell when it spawns the launcher; absent for
    //  interactive runs). See KzenLauncherMain.startManagedLifeline.
    val managedLifeline: Boolean = false,
    val parentPid: Long? = null,

    // Dev-only: when true, the launcher serves the /shell/project[/start|/stop] endpoints itself from
    //  an in-memory ShellSimulator instead of relying on a real kzen-shell in front. Set by
    //  FrontendDevelopment; false in production (where kzen-shell owns those routes). See ShellSimulator.
    val simulateShell: Boolean = false,

    // Version + build timestamp of the running artifact, loaded from a baked-in classpath resource
    //  (see BuildInfo). Surfaced to the client via indexPage as logo hover text.
    val buildInfo: BuildInfo? = null
) {
    //-----------------------------------------------------------------------------------------------------------------
    companion object {
        @Suppress("ConstPropertyName")
        private const val serverPortPrefix = "--server.port="

        private val serverPortRegex = Regex(
            Regex.escape(serverPortPrefix) + "\\d+")

        @Suppress("ConstPropertyName")
        private const val managedLifelinePrefix = "--managed.lifeline="

        @Suppress("ConstPropertyName")
        private const val parentPidPrefix = "--parent.pid="

        fun readPort(args: Array<String>): Int? {
            val match = args
                .lastOrNull { it.matches(serverPortRegex) }
                ?: return null

            val portText = match.substring(serverPortPrefix.length)
            return portText.toInt()
        }

        fun readManagedLifeline(args: Array<String>): Boolean {
            val match = args
                .lastOrNull { it.startsWith(managedLifelinePrefix) }
                ?: return false

            return match.substring(managedLifelinePrefix.length) == "stdin"
        }

        fun readParentPid(args: Array<String>): Long? {
            val match = args
                .lastOrNull { it.startsWith(parentPidPrefix) }
                ?: return null

            return match.substring(parentPidPrefix.length).toLongOrNull()
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
    val shellSimulator: ShellSimulator,
) {
    fun init() {
        archetypeRepo.init()
    }
}



//---------------------------------------------------------------------------------------------------------------------
const val kzenLauncherJsModuleName = "kzen-launcher-js"

//const val jsResourcePath = "$staticResourcePath/$jsFileName"

private const val indexFileName = "index.html"
private const val indexFilePath = "/$indexFileName"


// Resolve the project-archetype source from kzen-launcher.properties (a bundled classpath resource,
//  readable regardless of the launcher's working directory). Candidates are tried in order: a local
//  path is used only if it exists (resolved against the working directory, which differs between a
//  standalone run and a kzen-shell-spawned run); an http(s) URL is used as-is. See that file for the
//  candidate list and rationale.
private fun resolveArchetypeUrl(): String {
    val candidatePrefix = "archetype.project."

    val properties = Properties()
    KzenLauncherContext::class.java.getResourceAsStream("/kzen-launcher.properties")?.use {
        properties.load(it)
    }

    val candidates = properties.stringPropertyNames()
        .filter { it.startsWith(candidatePrefix) }
        .sortedBy { it.removePrefix(candidatePrefix).toIntOrNull() ?: Int.MAX_VALUE }
        .map { properties.getProperty(it) }

    for (candidate in candidates) {
        val lower = candidate.lowercase()
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("file:")) {
            return candidate
        }

        val path = Paths.get(candidate)
        if (Files.exists(path)) {
            return path.toAbsolutePath().normalize().toUri().toString()
        }
    }

    error("No project archetype source resolved from kzen-launcher.properties: $candidates")
}


// The archetype's version, read from its artifact filename (kzen-project-<version>.zip), so the new-
//  project screen shows which project build a new project is created from (0.29.1, or -SNAPSHOT in dev).
private fun archetypeVersion(archetypeUrl: String): String {
    return archetypeUrl
        .substringAfterLast('/')
        .removePrefix("kzen-project-")
        .removeSuffix(".zip")
}


//---------------------------------------------------------------------------------------------------------------------
fun buildContext(args: Array<String>): KzenLauncherContext {
    val kzenProperties = KzenProperties()
    val archetypeUrl = resolveArchetypeUrl()
    val projectArchetype = KzenProperties.Archetype()
    projectArchetype.name = "kzen-project"
    projectArchetype.title = "Automation and Reporting"
    projectArchetype.description =
        "Visually control a browser and more - v${archetypeVersion(archetypeUrl)}"
    projectArchetype.url = archetypeUrl
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
        port = port,
        managedLifeline = KzenLauncherConfig.readManagedLifeline(args),
        parentPid = KzenLauncherConfig.readParentPid(args),
        buildInfo = BuildInfo.load("/kzen-launcher-build.properties")
    )

    return KzenLauncherContext(
        config, restHandler, downloadService, archetypeRepo, ShellSimulator())
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

    SecurityGate.install(this)

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
        // The static route below revalidates via no-cache; the page itself must too, or a
        //  heuristically-cached index.html keeps serving a stale kzen-build meta (the version
        //  display) and bundle reference after an upgrade.
        call.response.headers.append(HttpHeaders.CacheControl, "no-cache")
        call.respondHtml(HttpStatusCode.OK) {
            indexPage(context.config)
        }
    }

    // Revalidate the SPA bundle (and all static assets) on every load so an upgraded build is picked
    //  up instead of a stale cached copy. If ever served over the internet / a CDN, switch to
    //  content-hashed immutable filenames (esbuild [hash]) to avoid the per-load revalidation.
    staticResources(staticResourcePath, staticResourceDir) {
        cacheControl { listOf(CacheControl.NoCache(null)) }
    }

    routeRest(context.restApi)

    // Dev only: stand in for kzen-shell's /shell/project endpoints when running standalone. In
    //  production kzen-shell owns these routes (it reverse-proxies the launcher), so they are not
    //  registered here.
    if (context.config.simulateShell) {
        routeShellSimulator(context.shellSimulator)
    }
}


private fun Routing.routeShellSimulator(
    shellSimulator: ShellSimulator
) {
    get(CommonRestApi.shellProject) {
        call.respond(shellSimulator.list())
    }
    get(CommonRestApi.startProject) {
        val name = call.parameters[CommonRestApi.projectName]
        if (name == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }
        shellSimulator.start(name)
        call.respondText("started")
    }
    get(CommonRestApi.stopProject) {
        val name = call.parameters[CommonRestApi.projectName]
        if (name == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }
        call.respond(shellSimulator.stop(name))
    }
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
        respondCommand { restHandler.createProject(call.parameters) }
    }
    get(CommonRestApi.importProject) {
        respondCommand { restHandler.importProject(call.parameters) }
    }
    get(CommonRestApi.removeProject) {
        respondCommand { restHandler.removeProject(call.parameters) }
    }
    get(CommonRestApi.deleteProject) {
        respondCommand { restHandler.deleteProject(call.parameters) }
    }
    get(CommonRestApi.renameProject) {
        respondCommand { restHandler.renameProject(call.parameters) }
    }
    get(CommonRestApi.jvmArgumentsProject) {
        respondCommand { restHandler.jvmArgumentsProject(call.parameters) }
    }
}


// Command failures are user-actionable, not server bugs: IllegalArgumentException (missing or
//  malformed params, rejected project names) → 400; IllegalStateException (state conflicts —
//  project already exists, directory still locked by a running project) → 409. Both would
//  otherwise surface as a message-less generic 500. JSON with a `message` field is the error
//  shape the client parses (see ajaxUtil.httpGet).
private suspend fun RoutingContext.respondCommand(command: () -> Unit) {
    try {
        command()
        call.response.status(HttpStatusCode.OK)
    }
    catch (e: IllegalArgumentException) {
        call.respond(HttpStatusCode.BadRequest, mapOf("message" to (e.message ?: "invalid request")))
    }
    catch (e: IllegalStateException) {
        call.respond(HttpStatusCode.Conflict, mapOf("message" to (e.message ?: "conflicting state")))
    }
}
