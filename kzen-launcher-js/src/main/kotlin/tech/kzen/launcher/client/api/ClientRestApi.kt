package tech.kzen.launcher.client.api


import kotlinext.js.getOwnPropertyNames
import tech.kzen.launcher.client.service.ErrorBus
import tech.kzen.launcher.common.CommonApi
import tech.kzen.launcher.common.dto.ProjectDetail
import kotlin.js.Json



class ClientRestApi(private val baseUrl: String, private val baseWsUrl: String) {
    suspend fun listArtifacts(): Map<String, String> {
        val artifactList = getWithErrorIntercept("$baseUrl${CommonApi.listArchetypes}")

        val artifacts = JSON.parse<Json>(artifactList)

        val nameToUrl = mutableMapOf<String, String>()
        for (property in artifacts.getOwnPropertyNames()) {
            nameToUrl[property] = artifacts[property] as String
        }

        return nameToUrl
    }


    suspend fun listProjects(): List<ProjectDetail> {
        val projectList = getWithErrorIntercept("$baseUrl${CommonApi.listProjects}")

        val projects = JSON.parse<Array<Json>>(projectList)

        val parsed = mutableListOf<ProjectDetail>()

        for (project in projects) {
            parsed.add(ProjectDetail(
                    project["name"] as String,
                    project["path"] as String,
                    project["exists"] as Boolean
            ))
        }

        return parsed
    }


    suspend fun createProject(name: String, type: String) {
        val encodedName = encodeURIComponent(name)
        val encodedType = encodeURIComponent(type)

        getWithErrorIntercept("$baseUrl${CommonApi.createProject}" +
                "?${CommonApi.projectName}=$encodedName" +
                "&${CommonApi.createProjectType}=$encodedType")
    }


    suspend fun importProject(path: String) {
        val encodedPath = encodeURIComponent(path)

        getWithErrorIntercept("$baseUrl${CommonApi.importProject}" +
                "?${CommonApi.importProjectPath}=$encodedPath")
    }


    suspend fun removeProject(name: String) {
        val encodedName = encodeURIComponent(name)
        getWithErrorIntercept("$baseUrl${CommonApi.removeProject}" +
                "?${CommonApi.projectName}=$encodedName")
    }


    suspend fun deleteProject(name: String) {
        val encodedName = encodeURIComponent(name)
        getWithErrorIntercept("$baseUrl${CommonApi.deleteProject}" +
                "?${CommonApi.projectName}=$encodedName")
    }


//    suspend fun startProject(name: String, location: String) {
//        val encodedName = encodeURIComponent(name)
//        val encodedLocation = encodeURIComponent(location)
//        httpGet("$baseUrl${CommonApi.createProject}?name=$encodedName&location=$encodedLocation")
//    }

    private suspend fun getWithErrorIntercept(url: String): String {
        try {
            val value = httpGet(url)
            ErrorBus.onSuccess()
            return value
        }
        catch (e: Exception) {
            console.log("@#!@#!@#!@ getWithErrorIntercept", e)
            ErrorBus.onError(e.message ?: "failed: $url")
            throw e
        }
    }
}


