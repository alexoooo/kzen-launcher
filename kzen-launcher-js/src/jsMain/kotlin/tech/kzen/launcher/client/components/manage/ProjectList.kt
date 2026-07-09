package tech.kzen.launcher.client.components.manage


import emotion.react.css
import mui.material.Divider
import react.ChildrenBuilder
import react.Key
import react.Props
import react.State
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.span
import tech.kzen.launcher.client.api.clientRestApi
import tech.kzen.launcher.client.api.launchUiAction
import tech.kzen.launcher.client.api.shellRestApi
import tech.kzen.launcher.client.components.sectionHeading
import tech.kzen.launcher.client.state.LauncherStore
import tech.kzen.launcher.client.wrap.RComponent
import tech.kzen.launcher.client.wrap.react
import tech.kzen.launcher.common.dto.ProjectDetail
import web.cssom.em
import kotlin.js.Promise


//---------------------------------------------------------------------------------------------------------------------
external interface ProjectListProps: Props {
    var projects: List<ProjectDetail>?
}


//---------------------------------------------------------------------------------------------------------------------
@Suppress("unused")
class ProjectList(
        props: ProjectListProps
): RComponent<ProjectListProps, State>(props) {
    //-----------------------------------------------------------------------------------------------------------------
    // Fire-and-forget: the shell registers the project as "starting" and returns immediately; the
    //  running-projects poll (and this eager refresh) surface the starting -> running transition. No
    //  local spinner — the project's state now lives on the server and shows in the Running section.
    private fun onStart(project: ProjectDetail) {
        launchUiAction {
            shellRestApi.startProject(project.name, project.path, project.jvmArgs)
            LauncherStore.invalidateRunning()
        }
    }


    private fun onRemove(project: ProjectDetail) {
        launchUiAction {
            clientRestApi.removeProject(project.name)
            LauncherStore.invalidateProjects()
        }
    }


    // Delegates to the store, which returns a Promise so ProjectItem can show a delete spinner until it
    //  settles. The store also refreshes the project list, dropping the deleted project (and its row).
    private fun onDelete(project: ProjectDetail): Promise<Unit> {
        return LauncherStore.deleteProject(project.name)
    }


    private fun onRename(project: ProjectDetail, newName: String) {
        launchUiAction {
            clientRestApi.renameProject(project.name, newName)
            LauncherStore.invalidateProjects()
        }
    }


    private fun onChangeJvmArguments(project: ProjectDetail, newArguments: String) {
        launchUiAction {
            clientRestApi.changeJvmArgumentsForProject(project.name, newArguments)
            LauncherStore.invalidateProjects()
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun ChildrenBuilder.render() {
        sectionHeading("Available Projects")

        val projects = props.projects
        if (projects != null) {
            if (projects.isEmpty()) {
                span {
                    css {
                        fontSize = 1.5.em
                    }
                    +"None, please add a New Project (top)"
                }
            }
            else {
                renderProjects(projects)
            }
        }
        else {
            +"Loading..."
        }
    }


    private fun ChildrenBuilder.renderProjects(projects: List<ProjectDetail>) {
        for (project in projects) {
            div {
                key = Key(project.name)

                Divider {}

                ProjectItem::class.react {
                    this.project = project

                    onStart = ::onStart
                    onRemove = ::onRemove
                    onDelete = ::onDelete
                    onRename = ::onRename
                    onChangeJvmArgs = ::onChangeJvmArguments
                }
            }
        }
    }
}
