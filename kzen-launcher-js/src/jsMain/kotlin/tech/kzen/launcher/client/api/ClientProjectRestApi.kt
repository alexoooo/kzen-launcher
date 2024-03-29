package tech.kzen.launcher.client.api


import tech.kzen.launcher.client.service.ClientRestService
import tech.kzen.launcher.client.util.ClientJsonUtils
import tech.kzen.launcher.common.api.CommonRestApi
import tech.kzen.launcher.common.dto.ArchetypeDetail
import tech.kzen.launcher.common.dto.ProjectDetail
import kotlin.js.Json



class ClientProjectRestApi(
        private val baseUrl: String,
//        private val baseWsUrl: String
) {
    suspend fun listArchetypes(): List<ArchetypeDetail> {
        val archetypeList = ClientRestService.getWithErrorIntercept("$baseUrl${CommonRestApi.listArchetypes}")

        val archetypesJson = JSON.parse<Array<Json>>(archetypeList)

        @Suppress("UNCHECKED_CAST")
        val archetypesCollection = ClientJsonUtils.toList(archetypesJson) as List<Map<String, String>>

        return archetypesCollection.map { ArchetypeDetail.fromCollection(it) }
    }


    suspend fun listProjects(): List<ProjectDetail> {
        val projectList = ClientRestService.getWithErrorIntercept("$baseUrl${CommonRestApi.listProjects}")

        val projects = JSON.parse<Array<Json>>(projectList)

        val parsed = mutableListOf<ProjectDetail>()

        for (project in projects) {
            parsed.add(ProjectDetail(
                    project[CommonRestApi.projectName] as String,
                    project[CommonRestApi.projectPath] as String,
                    project[CommonRestApi.projectJvmArgs] as String,
                    project[CommonRestApi.projectExists] as Boolean
            ))
        }

        return parsed
    }


    suspend fun createProject(name: String, type: String) {
        val encodedName = encodeURIComponent(name)
        val encodedType = encodeURIComponent(type)

        ClientRestService.getWithErrorIntercept("$baseUrl${CommonRestApi.createProject}" +
                "?${CommonRestApi.projectName}=$encodedName" +
                "&${CommonRestApi.createProjectType}=$encodedType")
    }


    suspend fun importProject(path: String) {
        val encodedPath = encodeURIComponent(path)

        ClientRestService.getWithErrorIntercept("$baseUrl${CommonRestApi.importProject}" +
                "?${CommonRestApi.projectPath}=$encodedPath")
    }


    suspend fun removeProject(name: String) {
        val encodedName = encodeURIComponent(name)
        ClientRestService.getWithErrorIntercept("$baseUrl${CommonRestApi.removeProject}" +
                "?${CommonRestApi.projectName}=$encodedName")
    }


    suspend fun deleteProject(name: String) {
        val encodedName = encodeURIComponent(name)
        ClientRestService.getWithErrorIntercept("$baseUrl${CommonRestApi.deleteProject}" +
                "?${CommonRestApi.projectName}=$encodedName")
    }


    suspend fun renameProject(name: String, newName: String) {
        val encodedName = encodeURIComponent(name)
        val encodedNewName = encodeURIComponent(newName)
        ClientRestService.getWithErrorIntercept("$baseUrl${CommonRestApi.renameProject}" +
                "?${CommonRestApi.projectName}=$encodedName&" +
                "${CommonRestApi.projectNewName}=$encodedNewName")
    }


    suspend fun changeJvmArgumentsForProject(name: String, jvmArguments: String) {
        val encodedName = encodeURIComponent(name)
        val encodedJvmArguments = encodeURIComponent(jvmArguments)
        ClientRestService.getWithErrorIntercept("$baseUrl${CommonRestApi.jvmArgumentsProject}" +
                "?${CommonRestApi.projectName}=$encodedName&" +
                "${CommonRestApi.projectJvmArgs}=$encodedJvmArguments")
    }
}


