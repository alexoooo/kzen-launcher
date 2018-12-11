package tech.kzen.launcher.client.api

import tech.kzen.launcher.client.service.ClientRestService


/**
 * Points to container shell
 */
class ClientShellRestApi {
    private companion object {
        const val base = "/shell/project"
    }

    suspend fun runningProjects(): List<String> {
        val json = ClientRestService.getWithErrorIntercept(base)

        val projectNames = JSON.parse<Array<String>>(json)

        return projectNames.toList()
    }


    suspend fun startProject(name: String, location: String) {
        val encodedName = encodeURIComponent(name)
        val encodedLocation = encodeURIComponent(location)
        ClientRestService.getWithErrorIntercept("$base/start?name=$encodedName" +
                "&location=$encodedLocation")
    }

    suspend fun stopProject(name: String) {
        val encodedName = encodeURIComponent(name)
        ClientRestService.getWithErrorIntercept("$base/stop?name=$encodedName")
    }
}


