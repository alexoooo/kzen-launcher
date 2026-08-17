package tech.kzen.launcher.server.api

import io.ktor.http.*
import tech.kzen.launcher.common.api.CommonRestApi
import tech.kzen.launcher.common.dto.ArchetypeDetail
import tech.kzen.launcher.common.dto.ProjectDetail
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
    fun listArchetypes(): List<ArchetypeDetail> {
        return archetypeRepo
            .all()
            .map {
                ArchetypeDetail(
                    name = it.key,
                    title = it.value.title,
                    description = it.value.description,
                    location = it.value.location.toAbsolutePath().normalize().toString(),
                    archetype = it.value.archetype,
                    version = it.value.version)
            }
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

        // The name is derived from the path's last segment, so it needs the same validation as a
        //  user-typed name — e.g. importing a directory named "main" or "shell" would collide with
        //  kzen-shell's reserved routing prefixes.
        ProjectNameValidation.check(projectName)

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
}