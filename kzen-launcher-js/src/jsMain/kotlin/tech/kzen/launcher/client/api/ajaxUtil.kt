package tech.kzen.launcher.client.api


import kotlinx.browser.window
import org.w3c.xhr.XMLHttpRequest
import kotlin.coroutines.*
import kotlin.js.Json
import kotlin.js.Promise


external fun encodeURIComponent(str: String): String


private val spaRoot = window.location.pathname.substringBeforeLast("/")
val clientRestApi = ClientProjectRestApi(
        baseUrl = spaRoot,
//        baseWsUrl = getWsServer()
)

val shellRestApi = ClientShellRestApi()


//private fun getWsServer(): String {
//    val location = window.location
//    val wsProtocol = if (location.protocol == "https:") "wss" else "ws"
//    return "$wsProtocol://${location.host}"
//}


suspend fun httpGet(url: String): String = suspendCoroutine { c ->
//    console.log("^^^ httpGet", url)

    val xhr = XMLHttpRequest()
    xhr.onreadystatechange = {
        if (xhr.readyState == XMLHttpRequest.DONE) {
            if (xhr.status / 100 == 2) {
                c.resume(xhr.response as String)
            }
            else {
                val response = JSON.parse<Json>(xhr.responseText)

                val message = response["message"]
                        as? String
                        ?: "${xhr.status} - ${xhr.responseText}"

//                console.log("^^^^^^^^^^^%^%^%^ xhr.response", message)
                c.resumeWithException(RuntimeException(message))
            }
        }
        null
    }
    xhr.open("GET", url)
    xhr.send()
}


fun <T> async(x: suspend () -> T): Promise<T> {
    return Promise { resolve, reject ->
        x.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(result: Result<T>) {
                if (result.isSuccess) {
                    resolve(result.getOrThrow())
                }
                else {
                    reject(result.exceptionOrNull()!!)
                }
            }
        })
    }
}