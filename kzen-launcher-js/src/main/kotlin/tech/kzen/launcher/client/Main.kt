package tech.kzen.launcher.client

import react.dom.br
import react.dom.div
import react.dom.render
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.api.clientRestApi
import tech.kzen.launcher.client.api.shellRestApi
import tech.kzen.launcher.client.components.ProjectCreate
import tech.kzen.launcher.client.components.ProjectList
import tech.kzen.launcher.client.components.ProjectRunning
import kotlin.browser.document
import kotlin.browser.window


fun main(args: Array<String>) {
    val pathname = window.location.pathname
    val withoutFile = pathname.substringBeforeLast("/")
    console.log("^^^^", withoutFile)

    window.onload = {
        async {
            val artifacts = clientRestApi.listArtifacts()
            console.log("$$ artifacts: $artifacts")

            val projects = clientRestApi.listProjects()
            console.log("$$ projects: $projects")

            val running = shellRestApi.runningProjects()
            console.log("$$ running: $running")

            val rootElement = document.getElementById("root")
                    ?: throw IllegalStateException("'root' element not found")

            render(rootElement) {
                div {
                    child(ProjectRunning::class) {
                        attrs.projects = running
                    }
                }

                br {}

                div {
                    child(ProjectCreate::class) {
                        attrs.artifacts = artifacts
                    }
                }

                br {}

                div {
                    child(ProjectList::class) {
                        attrs.projects = projects.filterKeys { ! running.contains(it) }
                    }
                }
            }
        }
    }
}
