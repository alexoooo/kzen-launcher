package tech.kzen.launcher.client.api

import kotlinx.serialization.decodeFromString
import tech.kzen.launcher.client.service.ClientRestService
import tech.kzen.launcher.common.api.CommonRestApi


/**
 * Points to container shell
 */
class ClientShellRestApi {
    //-----------------------------------------------------------------------------------------------------------------
    suspend fun runningProjects(): List<String> {
        return clientJson.decodeFromString(get(CommonRestApi.shellProject))
    }


    suspend fun startProject(name: String, location: String, jvmArgs: String) {
        get(CommonRestApi.startProject,
                CommonRestApi.projectName to name,
                CommonRestApi.projectLocation to location,
                CommonRestApi.projectJvmArgs to jvmArgs)
    }


    suspend fun stopProject(name: String) {
        get(CommonRestApi.stopProject,
                CommonRestApi.projectName to name)
    }


    //-----------------------------------------------------------------------------------------------------------------
    private suspend fun get(path: String, vararg params: Pair<String, String>): String {
        return ClientRestService.getWithErrorIntercept(restUrl(path, *params))
    }
}
