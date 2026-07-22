package tech.kzen.launcher.common.api


object CommonRestApi {
    private const val restServicePrefix = "/rs/"

    private const val queryPrefix = "${restServicePrefix}query/"
    const val listArchetypes = "${queryPrefix}archetype"
    const val listProjects = "${queryPrefix}project"

    private const val commandPrefix = "${restServicePrefix}command/"
    private const val projectCommandPrefix = "${commandPrefix}project/"
    const val createProject = "${projectCommandPrefix}create"
    const val importProject = "${projectCommandPrefix}import"
    const val upgradeProject = "${projectCommandPrefix}upgrade"
    const val removeProject = "${projectCommandPrefix}remove"
    const val deleteProject = "${projectCommandPrefix}delete"
    const val renameProject = "${projectCommandPrefix}rename"
    const val jvmArgumentsProject = "${projectCommandPrefix}args"

    // Shell (container reverse-proxy) endpoints — served by kzen-shell, in front of the launcher.
    const val shellProject = "/shell/project"
    const val startProject = "${shellProject}/start"
    const val stopProject = "${shellProject}/stop"

    const val projectName = "name"
    const val projectPath = "path"
    const val projectLocation = "location"
    const val projectJvmArgs = "args"
    const val createProjectType = "type"
    const val projectNewName = "new"
}

