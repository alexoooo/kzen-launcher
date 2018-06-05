package tech.kzen.launcher.client

import react.dom.div
import react.dom.render
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.api.restApi
import tech.kzen.launcher.client.components.ProjectCreate
import tech.kzen.launcher.client.components.ProjectList
import kotlin.browser.document
import kotlin.browser.window


fun main(args: Array<String>) {
    val pathname = window.location.pathname
    val withoutFile = pathname.substringBeforeLast("/")
    console.log("^^^^", withoutFile)

    window.onload = {
        async {
            val artifacts = restApi.listArtifacts()
            console.log("$$ artifacts: $artifacts")

            val projects = restApi.listProjects()
            console.log("$$ projects: $projects")

            val rootElement = document.getElementById("root")
                    ?: throw IllegalStateException("'root' element not found")

            render(rootElement) {
                div {
                    child(ProjectCreate::class) {
                        attrs.artifacts = artifacts
                    }
                }

                div {
                    child(ProjectList::class) {
                        attrs.projects = projects
                    }
                }
            }
        }
    }
}
