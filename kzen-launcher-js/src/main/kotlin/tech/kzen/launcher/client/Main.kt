package tech.kzen.launcher.client

import kotlinx.html.dom.create
import kotlinx.html.js.div
import kotlinx.html.p
import org.w3c.dom.get
import react.dom.div
import react.dom.render
import tech.kzen.launcher.client.components.HelloComponent
import tech.kzen.launcher.common.getAnswer
import kotlin.browser.document
import kotlin.browser.window


fun main(args: Array<String>) {
    window.onload = {
        render(document.getElementById("root")!!) {
            div {
                child(HelloComponent::class) {
                    attrs.name = "Fooo: ${getAnswer()}"
                }
            }
        }
    }
}
