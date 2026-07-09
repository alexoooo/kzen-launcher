package tech.kzen.launcher.client.api

import kotlinx.serialization.decodeFromString
import tech.kzen.launcher.client.service.ClientRestService
import tech.kzen.launcher.common.api.CommonRestApi
import tech.kzen.launcher.common.dto.RunningProject


/**
 * Points to container shell
 */
class ClientShellRestApi {
    //-----------------------------------------------------------------------------------------------------------------
    suspend fun runningProjects(): List<RunningProject> {
        return clientJson.decodeFromString(get(CommonRestApi.shellProject))
    }


    // Same fetch, but WITHOUT the ErrorBus success/error interception. Used by the background poll loop
    //  so a routine tick neither clears an error banner the user should still see (on success) nor spams
    //  one (on a transient failure). Interactive calls use runningProjects().
    suspend fun runningProjectsSilent(): List<RunningProject> {
        return clientJson.decodeFromString(httpGet(restUrl(CommonRestApi.shellProject)))
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
