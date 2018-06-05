package tech.kzen.launcher.common


object CommonApi {
    val listArchetypes = "/rs/query/archetype"

//    fun createProject(path)
    val createProject = "/rs/command/project/create"
    val createProjectName = "name"
    val createProjectType = "type"

    val listProjects = "/rs/query/project"
}

