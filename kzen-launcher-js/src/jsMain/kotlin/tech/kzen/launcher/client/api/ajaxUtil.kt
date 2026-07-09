package tech.kzen.launcher.client.api


import kotlinx.browser.window
import kotlinx.serialization.json.Json
import org.w3c.xhr.XMLHttpRequest
import kotlin.coroutines.*
import kotlin.js.Json as JsonObject
import kotlin.js.Promise


external fun encodeURIComponent(str: String): String


// Shared JSON codec for decoding REST responses into the @Serializable common DTOs. `ignoreUnknownKeys`
// so a field the server adds later doesn't break the client.
val clientJson = Json {
    ignoreUnknownKeys = true
}


// Build a GET URL from a path and URL-encoded query params (no params → the bare path). Centralizes the
// per-method hand-concatenation the REST clients used to repeat.
fun restUrl(path: String, vararg params: Pair<String, String>): String {
    if (params.isEmpty()) {
        return path
    }

    val query = params.joinToString("&") { (key, value) ->
        "$key=${encodeURIComponent(value)}"
    }
    return "$path?$query"
}


private val spaRoot = window.location.pathname.substringBeforeLast("/")
val clientRestApi = ClientProjectRestApi(
        baseUrl = spaRoot
)

val shellRestApi = ClientShellRestApi()


suspend fun httpGet(url: String): String = suspendCoroutine { c ->
    val xhr = XMLHttpRequest()
    xhr.onreadystatechange = {
        if (xhr.readyState == XMLHttpRequest.DONE) {
            if (xhr.status / 100 == 2) {
                c.resume(xhr.response as String)
            }
            else {
                // The error body is usually JSON with a `message` field, but a proxy 502 / HTML error
                // page is not JSON — guard the parse so a SyntaxError here doesn't mask the real status.
                val message = try {
                    JSON.parse<JsonObject>(xhr.responseText)["message"] as? String
                }
                catch (e: Throwable) {
                    null
                } ?: "${xhr.status} - ${xhr.responseText}"

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


// Launch a fire-and-forget coroutine for a UI action (button click, etc.). A rejected `async {}` with no
// attached handler becomes an unhandled promise rejection; this catches at the coroutine boundary so that
// never happens. Network errors are already surfaced to the user via
// ClientRestService.getWithErrorIntercept (→ ErrorBus banner) before propagating here — the thrown
// exception still short-circuits the coroutine, so post-await success steps (e.g. a refresh callback)
// correctly don't run on failure. We log here too so a non-network bug (which never reaches the ErrorBus
// interceptor) stays visible in the console.
fun launchUiAction(block: suspend () -> Unit) {
    async {
        try {
            block()
        }
        catch (e: Throwable) {
            console.error("UI action failed", e)
        }
    }
}