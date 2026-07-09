package tech.kzen.launcher.client.components.manage


import emotion.react.css
import kotlinx.coroutines.delay
import mui.material.CircularProgress
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
import tech.kzen.launcher.client.wrap.setState
import tech.kzen.launcher.common.dto.ProjectDetail
import web.cssom.em
import web.cssom.px


//---------------------------------------------------------------------------------------------------------------------
external interface ProjectListProps: Props {
    var projects: List<ProjectDetail>?
}


external interface ProjectListState: State {
    var starting: Boolean
}


//---------------------------------------------------------------------------------------------------------------------
@Suppress("unused")
class ProjectList(
        props: ProjectListProps
): RComponent<ProjectListProps, ProjectListState>(props) {
    //-----------------------------------------------------------------------------------------------------------------
    override fun ProjectListState.init(props: ProjectListProps) {
        starting = false
    }


    //-----------------------------------------------------------------------------------------------------------------
    private fun onStart(project: ProjectDetail) {
        setState {
            starting = true
        }

        launchUiAction {
            delay(1)

            try {
                shellRestApi.startProject(project.name, project.path, project.jvmArgs)
                LauncherStore.invalidateRunning()
            }
            finally {
                setState {
                    starting = false
                }
            }
        }
    }


    private fun onRemove(project: ProjectDetail) {
        launchUiAction {
            clientRestApi.removeProject(project.name)
            LauncherStore.invalidateProjects()
        }
    }


    private fun onDelete(project: ProjectDetail) {
        launchUiAction {
            clientRestApi.deleteProject(project.name)
            LauncherStore.invalidateProjects()
        }
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

        if (state.starting) {
            span {
                css {
                    float = web.cssom.Float.right
                    marginTop = (-55).px
                }
                CircularProgress {}
            }
        }

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
                    starting = state.starting

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
