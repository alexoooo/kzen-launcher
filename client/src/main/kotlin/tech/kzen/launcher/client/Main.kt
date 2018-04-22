package tech.kzen.launcher.client

import kotlinx.html.dom.create
import kotlinx.html.js.div
import kotlinx.html.p
import org.w3c.dom.get
import tech.kzen.launcher.common.getAnswer
import kotlin.browser.document


fun main(args: Array<String>) {
    val message = document.create.div {
        p {
            +"kkkkk: ${getAnswer()}"
        }
    }

    val body = document.getElementsByTagName("body")[0]!!
    body.appendChild(message)
}
