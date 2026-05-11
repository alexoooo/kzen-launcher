package tech.kzen.launcher.client.service

import tech.kzen.launcher.client.api.httpGet


object ClientRestService {
    suspend fun getWithErrorIntercept(url: String): String {
        try {
            val value = httpGet(url)
            ErrorBus.onSuccess()
            return value
        }
        catch (e: Exception) {
            console.error("getWithErrorIntercept failed:", url, e)
            ErrorBus.onError(e.message ?: "failed: $url")
            throw e
        }
    }
}