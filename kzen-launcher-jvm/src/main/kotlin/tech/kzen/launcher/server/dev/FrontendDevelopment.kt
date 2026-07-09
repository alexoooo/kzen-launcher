package tech.kzen.launcher.server.dev

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import tech.kzen.launcher.server.KzenLauncherContext
import tech.kzen.launcher.server.buildContext
import tech.kzen.launcher.server.ktorMain
import java.nio.file.Path


private val logger = LoggerFactory.getLogger("tech.kzen.launcher.server.dev.FrontendDevelopment")


fun main(args: Array<String>) {
    val base = buildContext(args)
    // Stand up the in-memory shell simulator so /shell/project[/start|/stop] work without a real
    //  kzen-shell in front (production has the shell; a standalone dev run does not).
    val context = base.copy(config = base.config.copy(simulateShell = true))
    context.init()
    frontendDevelopmentMain(context)
}


fun frontendDevelopmentMain(
    context: KzenLauncherContext
) {
    System.setProperty("io.ktor.development", "true")

    val projectBaseDir = Path.of(".").toAbsolutePath().normalize()
    val jsDistDir = projectBaseDir.resolve(
//        "${context.config.jsModuleName}/build/distributions")
        "${context.config.jsModuleName}/build/dist/js/productionExecutable")
    val jsFile = jsDistDir.resolve(context.config.jsFileName()).toFile()
    logger.info("Auto-reload js file (exists = {}): {}", jsFile.exists(), jsFile)

    embeddedServer(
        Netty,
        port = context.config.port,
        host = context.config.host
    ) {
        routing {
            get(context.config.jsResourcePath()) {
                call.respondFile(jsFile)
            }
        }

        ktorMain(context)
    }.start(wait = true)
}