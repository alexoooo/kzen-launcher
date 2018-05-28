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
    window.onload = {
        async {
            val artifacts = restApi.artifacts()

            render(document.getElementById("root")!!) {
                div {
                    child(ProjectCreate::class) {
                        attrs.projects = artifacts
                    }
                }
            }
        }
    }
}
