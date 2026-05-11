package tech.kzen.launcher.client

import kotlinx.browser.window
import react.Fragment
import react.create
import react.dom.client.createRoot
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.components.ProjectLauncher
import tech.kzen.launcher.client.wrap.react
import tech.kzen.launcher.common.api.rootHtmlElementId
import web.dom.ElementId
import web.html.HTMLElement


fun main() {
    fun emptyRootElement(): HTMLElement {
        val rootElement = web.dom.document.getElementById(ElementId(rootHtmlElementId))
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
