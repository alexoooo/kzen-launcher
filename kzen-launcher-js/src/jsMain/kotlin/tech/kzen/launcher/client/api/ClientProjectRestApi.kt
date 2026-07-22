package tech.kzen.launcher.client.api


import kotlinx.serialization.decodeFromString
import tech.kzen.launcher.client.service.ClientRestService
import tech.kzen.launcher.common.api.CommonRestApi
import tech.kzen.launcher.common.dto.ArchetypeDetail
import tech.kzen.launcher.common.dto.ProjectDetail



class ClientProjectRestApi(
        private val baseUrl: String
) {
    suspend fun listArchetypes(): List<ArchetypeDetail> {
        return clientJson.decodeFromString(get(CommonRestApi.listArchetypes))
    }


    suspend fun listProjects(): List<ProjectDetail> {
        return clientJson.decodeFromString(get(CommonRestApi.listProjects))
    }


    // Same fetch, but WITHOUT the ErrorBus success/error interception — for the background poll loop, so
    //  a routine tick neither clears an error banner the user should still see nor spams one on a transient
    //  failure. Mirrors ClientShellRestApi.runningProjectsSilent(). Interactive calls use listProjects().
    suspend fun listProjectsSilent(): List<ProjectDetail> {
        return clientJson.decodeFromString(httpGet("$baseUrl${restUrl(CommonRestApi.listProjects)}"))
    }


    suspend fun createProject(name: String, type: String) {
        get(CommonRestApi.createProject,
                CommonRestApi.projectName to name,
                CommonRestApi.createProjectType to type)
    }


    suspend fun importProject(path: String) {
        get(CommonRestApi.importProject,
                CommonRestApi.projectPath to path)
    }


    suspend fun upgradeProject(name: String, type: String) {
        get(CommonRestApi.upgradeProject,
                CommonRestApi.projectName to name,
                CommonRestApi.createProjectType to type)
    }


    suspend fun removeProject(name: String) {
        get(CommonRestApi.removeProject,
                CommonRestApi.projectName to name)
    }


    suspend fun deleteProject(name: String) {
        get(CommonRestApi.deleteProject,
                CommonRestApi.projectName to name)
    }


    suspend fun renameProject(name: String, newName: String) {
        get(CommonRestApi.renameProject,
                CommonRestApi.projectName to name,
                CommonRestApi.projectNewName to newName)
    }


    suspend fun changeJvmArgumentsForProject(name: String, jvmArguments: String) {
        get(CommonRestApi.jvmArgumentsProject,
                CommonRestApi.projectName to name,
                CommonRestApi.projectJvmArgs to jvmArguments)
    }


    private suspend fun get(path: String, vararg params: Pair<String, String>): String {
        return ClientRestService.getWithErrorIntercept("$baseUrl${restUrl(path, *params)}")
    }
}
