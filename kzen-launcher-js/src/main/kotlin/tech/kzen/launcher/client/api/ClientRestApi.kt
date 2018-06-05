package tech.kzen.launcher.client.api


import kotlinext.js.getOwnPropertyNames
import tech.kzen.launcher.common.CommonApi
import kotlin.js.Json
//import kotlinx.serialization.json.JSON as KJSON



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


//    suspend fun scan(): List<ProjectPath> {
//        val scanText = httpGet("$baseUrl/scan")
////        println("scanText: $scanText")
//
//        val builder = mutableListOf<ProjectPath>()
//
//        JSON.parse<Array<Json>>(scanText)
//                .map { it["relativeLocation"] as String }
//                .mapTo(builder) { ProjectPath(it) }
//
//        return builder
//
////        val parsed = JSON.parse<List<ProjectPath>>(scanText)
////        console.log("parsed", parsed)
//
//        //val parsed: List<ProjectPath> = JSON.parse(scanText)
//        //println("parsed: $parsed")
////        return parsed
//
////        return listOf(
////                ProjectPath("notation.yaml"))
//    }
}


