package tech.kzen.launcher.common


object CommonApi {
    const val listArchetypes = "/rs/query/archetype"

    const val createProject = "/rs/command/project/create"
    const val importProject = "/rs/command/project/import"
    const val removeProject = "/rs/command/project/remove"
    const val deleteProject = "/rs/command/project/delete"
    const val projectName = "name"
    const val createProjectType = "type"
    const val importProjectPath = "path"

    const val listProjects = "/rs/query/project"
}

