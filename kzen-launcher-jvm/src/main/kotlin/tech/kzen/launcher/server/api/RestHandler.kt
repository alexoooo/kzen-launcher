package tech.kzen.launcher.server.api

import com.google.common.collect.ImmutableMap
import io.ktor.http.*
import tech.kzen.launcher.common.api.CommonRestApi
import tech.kzen.launcher.common.dto.ProjectDetail
import tech.kzen.launcher.server.archetype.ArchetypeInfo
import tech.kzen.launcher.server.archetype.ArchetypeRepo
import tech.kzen.launcher.server.project.ProjectCreator
import tech.kzen.launcher.server.project.ProjectNameValidation
import tech.kzen.launcher.server.project.ProjectRepo
import java.nio.file.Files
import java.nio.file.Paths


class RestHandler(
    private val archetypeRepo: ArchetypeRepo,
    private val projectRepo: ProjectRepo,
    private val projectCreator: ProjectCreator
) {
    //-----------------------------------------------------------------------------------------------------------------
    fun listArchetypes(): ImmutableMap<String, ArchetypeInfo> {
        return archetypeRepo.all()
    }


    fun listProjects(): List<ProjectDetail> {
        return projectRepo
            .all()
            .map {
                val path = it.value.home
                val normalized = path.toString().replace('\\', '/')
                val exists = Files.exists(path)

                ProjectDetail(
                    name = it.key,
                    path = normalized,
                    jvmArgs = it.value.jvmArguments,
                    exists = exists,
                    archetype = it.value.archetype,
                    version = it.value.version)
            }
    }


    fun createProject(parameters: Parameters) {
        val projectName = parameters.getParam(CommonRestApi.projectName)
        val archetypeName = parameters.getParam(CommonRestApi.createProjectType)

        ProjectNameValidation.check(projectName)

        // Resolve the archetype once so its base name + version are recorded on the project — the same
        //  lookup ProjectCreator.create does internally (a scan lookup, not a path resolve; safe input).
        val archetypeInfo = archetypeRepo.get(archetypeName)

        val projectHome = projectCreator.create(projectName, archetypeName)
        projectRepo.add(projectName, projectHome, archetypeInfo.archetype, archetypeInfo.version)
    }


    fun importProject(parameters: Parameters) {
        val projectHome = parameters.getParam(CommonRestApi.projectPath, Paths::get)
        val projectName = projectHome.fileName.toString()

        // An imported project has no known archetype source, so it records unknown/unknown (add's defaults).
        projectRepo.add(projectName, projectHome)
    }


    fun upgradeProject(parameters: Parameters) {
        val projectName = parameters.getParam(CommonRestApi.projectName)
        val archetypeName = parameters.getParam(CommonRestApi.createProjectType)

        val projectInfo = projectRepo.get(projectName)
        val archetypeInfo = archetypeRepo.get(archetypeName)

        projectCreator.upgrade(projectInfo.home, archetypeInfo)
        projectRepo.recordArchetype(projectName, archetypeInfo.archetype, archetypeInfo.version)
    }


    fun removeProject(parameters: Parameters) {
        val projectName = parameters.getParam(CommonRestApi.projectName)
        projectRepo.remove(projectName)
    }


    fun deleteProject(parameters: Parameters) {
        val projectName = parameters.getParam(CommonRestApi.projectName)
        projectRepo.delete(projectName)
    }


    fun renameProject(parameters: Parameters) {
        val projectName = parameters.getParam(CommonRestApi.projectName)
        val newName = parameters.getParam(CommonRestApi.projectNewName)

        ProjectNameValidation.check(newName)

        projectRepo.rename(projectName, newName)
    }


    fun jvmArgumentsProject(parameters: Parameters) {
        val projectName = parameters.getParam(CommonRestApi.projectName)
        val jvmArguments = parameters.getParam(CommonRestApi.projectJvmArgs)

        projectRepo.changeArguments(projectName, jvmArguments)
    }


    //-----------------------------------------------------------------------------------------------------------------
    private fun Parameters.getParam(
        parameterName: String
    ): String {
        return getParam(parameterName) { it }
    }

    private fun <T> Parameters.getParam(
        parameterName: String,
        parser: (String) -> T
    ): T {
        val queryParamValues: List<String>? = getAll(parameterName)
        require(!queryParamValues.isNullOrEmpty()) { "'$parameterName' required" }
        require(queryParamValues.size == 1) { "Single '$parameterName' expected: $queryParamValues" }
        return parser(queryParamValues.single())
    }


    private fun <T> Parameters.getParamList(
        parameterName: String,
        parser: (String) -> T
    ): List<T> {
        val queryParamValues: List<String> = getAll(parameterName)
            ?: return listOf()
        return queryParamValues.map(parser)
    }


    private fun <T> Parameters.getParamOrNull(
        parameterName: String,
        parser: (String) -> T
    ): T? {
        val queryParamValues: List<String> = getAll(parameterName)
            ?: return null

        require(queryParamValues.isNotEmpty()) { "'$parameterName' required" }
        require(queryParamValues.size == 1) { "Single '$parameterName' expected: $queryParamValues" }

        return parser(queryParamValues.single())
    }
}