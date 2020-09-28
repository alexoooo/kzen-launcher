package tech.kzen.launcher.client

import react.dom.render
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.components.ProjectLauncher
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.clear


fun main() {
//    val pathname = window.location.pathname
//    val withoutFile = pathname.substringBeforeLast("/")
//    console.log("^^^^", withoutFile)

    window.onload = {
        async {
            val rootElement = document.getElementById("root")
                    ?: throw IllegalStateException("'root' element not found")

            rootElement.clear()

            render(rootElement) {
                child(ProjectLauncher::class) {}
            }
        }
    }
}
