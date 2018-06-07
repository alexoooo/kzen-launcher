package tech.kzen.launcher.client.api


import kotlinext.js.getOwnPropertyNames
import tech.kzen.launcher.common.CommonApi
import kotlin.js.Json
//import kotlinx.serialization.json.JSON as KJSON


/**
 * Points to container shell
 */
class ShellRestApi {
    private companion object {
        const val base = "/shell/project"
    }

    suspend fun runningProjects(): List<String> {
        val json = httpGet(base)

        val projectNames = JSON.parse<Array<String>>(json)

        return projectNames.toList()
    }


    suspend fun startProject(name: String, location: String) {
        val encodedName = encodeURIComponent(name)
        val encodedLocation = encodeURIComponent(location)
        httpGet("$base/start?name=$encodedName&location=$encodedLocation")
    }

    suspend fun stopProject(name: String) {
        val encodedName = encodeURIComponent(name)
        httpGet("$base/stop?name=$encodedName")
    }
}


