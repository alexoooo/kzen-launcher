package tech.kzen.launcher.common


object CommonApi {
    private const val restServicePrefix = "/rs/"

    private const val queryPrefix = "${restServicePrefix}query/"
    const val listArchetypes = "${queryPrefix}archetype"
    const val listProjects = "${queryPrefix}project"

    private const val commandPrefix = "${restServicePrefix}command/"
    private const val projectCommandPrefix = "${commandPrefix}project/"
    const val createProject = "${projectCommandPrefix}create"
    const val importProject = "${projectCommandPrefix}import"
    const val removeProject = "${projectCommandPrefix}remove"
    const val deleteProject = "${projectCommandPrefix}delete"
    const val renameProject = "${projectCommandPrefix}rename"

    const val projectName = "name"
    const val createProjectType = "type"
    const val importProjectPath = "path"
    const val projectNewName = "new"
}

