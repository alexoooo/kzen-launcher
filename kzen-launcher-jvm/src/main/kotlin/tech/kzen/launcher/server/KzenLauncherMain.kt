package tech.kzen.launcher.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.reactive.config.EnableWebFlux


@EnableWebFlux
@SpringBootApplication
class KzenLauncherMain


fun main(args: Array<String>) {
    runApplication<KzenLauncherMain>(*args)
}
