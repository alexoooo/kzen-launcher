package tech.kzen.launcher.client.api


import tech.kzen.launcher.client.service.ClientRestService
import tech.kzen.launcher.client.util.ClientJsonUtils
import tech.kzen.launcher.common.CommonApi
import tech.kzen.launcher.common.dto.ArchetypeDetail
import tech.kzen.launcher.common.dto.ProjectDetail
import kotlin.js.Json



class ClientProjectRestApi(
        private val baseUrl: String,
        private val baseWsUrl: String
) {
    suspend fun listArtifacts(): List<ArchetypeDetail> {
        val artifactList = ClientRestService.getWithErrorIntercept("$baseUrl${CommonApi.listArchetypes}")

        val artifactsJson = JSON.parse<Array<Json>>(artifactList)

        @Suppress("UNCHECKED_CAST")
        val artifactsCollection = ClientJsonUtils.toList(artifactsJson) as List<Map<String, String>>


        return artifactsCollection.map { ArchetypeDetail.fromCollection(it) }
    }


    suspend fun listProjects(): List<ProjectDetail> {
        val projectList = ClientRestService.getWithErrorIntercept("$baseUrl${CommonApi.listProjects}")

        val projects = JSON.parse<Array<Json>>(projectList)

        val parsed = mutableListOf<ProjectDetail>()

        for (project in projects) {
            parsed.add(ProjectDetail(
                    project[CommonApi.projectName] as String,
                    project[CommonApi.projectPath] as String,
                    project[CommonApi.projectJvmArgs] as String,
                    project[CommonApi.projectExists] as Boolean
            ))
        }

        return parsed
    }


    suspend fun createProject(name: String, type: String) {
        val encodedName = encodeURIComponent(name)
        val encodedType = encodeURIComponent(type)

        ClientRestService.getWithErrorIntercept("$baseUrl${CommonApi.createProject}" +
                "?${CommonApi.projectName}=$encodedName" +
                "&${CommonApi.createProjectType}=$encodedType")
    }


    suspend fun importProject(path: String) {
        val encodedPath = encodeURIComponent(path)

        ClientRestService.getWithErrorIntercept("$baseUrl${CommonApi.importProject}" +
                "?${CommonApi.projectPath}=$encodedPath")
    }


    suspend fun removeProject(name: String) {
        val encodedName = encodeURIComponent(name)
        ClientRestService.getWithErrorIntercept("$baseUrl${CommonApi.removeProject}" +
                "?${CommonApi.projectName}=$encodedName")
    }


    suspend fun deleteProject(name: String) {
        val encodedName = encodeURIComponent(name)
        ClientRestService.getWithErrorIntercept("$baseUrl${CommonApi.deleteProject}" +
                "?${CommonApi.projectName}=$encodedName")
    }


    suspend fun renameProject(name: String, newName: String) {
        val encodedName = encodeURIComponent(name)
        val encodedNewName = encodeURIComponent(newName)
        ClientRestService.getWithErrorIntercept("$baseUrl${CommonApi.renameProject}" +
                "?${CommonApi.projectName}=$encodedName&" +
                "${CommonApi.projectNewName}=$encodedNewName")
    }


    suspend fun changeJvmArgumentsForProject(name: String, jvmArguments: String) {
        val encodedName = encodeURIComponent(name)
        val encodedJvmArguments = encodeURIComponent(jvmArguments)
        ClientRestService.getWithErrorIntercept("$baseUrl${CommonApi.jvmArgumentsProject}" +
                "?${CommonApi.projectName}=$encodedName&" +
                "${CommonApi.projectJvmArgs}=$encodedJvmArguments")
    }
}


