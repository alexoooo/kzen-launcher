package tech.kzen.launcher.common


object CommonApi {
    val listArchetypes = "/rs/query/archetype"

    val createProject = "/rs/command/project/create"
    val importProject = "/rs/command/project/import"
    val removeProject = "/rs/command/project/remove"
    val deleteProject = "/rs/command/project/delete"
    val projectName = "name"
    val createProjectType = "type"
    val importProjectPath = "path"

    val listProjects = "/rs/query/project"
}

