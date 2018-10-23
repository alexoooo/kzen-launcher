package tech.kzen.launcher.common


object CommonApi {
    val listArchetypes = "/rs/query/archetype"

//    fun createProject(path)
    val createProject = "/rs/command/project/create"
    val removeProject = "/rs/command/project/remove"
    val deleteProject = "/rs/command/project/delete"
    val projectName = "name"
    val createProjectType = "type"

    val listProjects = "/rs/query/project"
}

