package tech.kzen.launcher.client

import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.components.ProjectLauncher
import kotlinx.browser.window
import react.Fragment
import react.create
import react.dom.client.createRoot
import react.react
import tech.kzen.launcher.common.api.rootHtmlElementId
import web.html.HTMLElement


fun main() {
//    val pathname = window.location.pathname
//    val withoutFile = pathname.substringBeforeLast("/")
//    console.log("^^^^", withoutFile)

    fun emptyRootElement(): HTMLElement {
        val rootElement = web.dom.document.getElementById(rootHtmlElementId)
            ?: throw IllegalStateException("'$rootHtmlElementId' element not found")

        while (rootElement.hasChildNodes()) {
            rootElement.removeChild(rootElement.firstChild!!)
        }
        return rootElement
    }

    window.onload = {
        async {
            val rootElement = emptyRootElement()

            createRoot(rootElement).render(Fragment.create {
                ProjectLauncher::class.react {}
            })
        }
    }
}
