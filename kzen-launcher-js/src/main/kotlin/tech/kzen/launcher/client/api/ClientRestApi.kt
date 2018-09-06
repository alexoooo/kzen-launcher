package tech.kzen.launcher.client.api


import kotlinext.js.getOwnPropertyNames
import tech.kzen.launcher.common.CommonApi
import kotlin.js.Json



class ClientRestApi(private val baseUrl: String, private val baseWsUrl: String) {
    suspend fun listArtifacts(): Map<String, String> {
        val artifactList = httpGet("$baseUrl${CommonApi.listArchetypes}")

        val artifacts = JSON.parse<Json>(artifactList)

        val nameToUrl = mutableMapOf<String, String>()
        for (property in artifacts.getOwnPropertyNames()) {
            nameToUrl[property] = artifacts[property] as String
        }

        return nameToUrl
    }


    suspend fun listProjects(): Map<String, String> {
        val projectList = httpGet("$baseUrl${CommonApi.listProjects}")

        val artifacts = JSON.parse<Json>(projectList)

        val nameToUrl = mutableMapOf<String, String>()
        for (property in artifacts.getOwnPropertyNames()) {
            nameToUrl[property] = artifacts[property] as String
        }

        return nameToUrl
    }


    suspend fun createProject(name: String, type: String) {
        val encodedName = encodeURIComponent(name)
        val encodedType = encodeURIComponent(type)
        httpGet("$baseUrl${CommonApi.createProject}?name=$encodedName&type=$encodedType")
    }


    suspend fun startProject(name: String, location: String) {
        val encodedName = encodeURIComponent(name)
        val encodedLocation = encodeURIComponent(location)
        httpGet("$baseUrl${CommonApi.createProject}?name=$encodedName&location=$encodedLocation")
    }
}


