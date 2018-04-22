package tech.kzen.launcher.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.reactive.config.EnableWebFlux


@EnableWebFlux
@SpringBootApplication
class KzenLauncherApp


fun main(args: Array<String>) {
    runApplication<KzenLauncherApp>(*args)
}
