package tech.kzen.launcher.client.components.manage


import csstype.Float
import csstype.em
import csstype.px
import emotion.react.css
import kotlinx.coroutines.delay
import mui.material.CircularProgress
import mui.material.Divider
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.span
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.api.clientRestApi
import tech.kzen.launcher.client.api.shellRestApi
import tech.kzen.launcher.client.wrap.RComponent
import tech.kzen.launcher.client.wrap.setState
import tech.kzen.launcher.common.dto.ProjectDetail


//---------------------------------------------------------------------------------------------------------------------
external interface ProjectListProps: Props {
    var projects: List<ProjectDetail>?

    var didStart: (() -> Unit)?
    var didRemove: (() -> Unit)?
    var didDelete: (() -> Unit)?
    var didRename: (() -> Unit)?
    var didChangeJvmArgs: (() -> Unit)?
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

        async {
            delay(1)

            try {
                shellRestApi.startProject(project.name, project.path, project.jvmArgs)
                props.didStart?.invoke()
            }
            finally {
                setState {
                    starting = false
                }
            }
        }
    }


    private fun onRemove(project: ProjectDetail) {
//        console.log("onRemove: name - $name")
        async {
            clientRestApi.removeProject(project.name)
            props.didRemove?.invoke()
        }
    }


    private fun onDelete(project: ProjectDetail) {
//        console.log("onDelete: name - $name")
        async {
            clientRestApi.deleteProject(project.name)
            props.didDelete?.invoke()
        }
    }


    private fun onRename(project: ProjectDetail, newName: String) {
//        console.log("onDelete: name - $name")
        async {
            clientRestApi.renameProject(project.name, newName)
            props.didRename?.invoke()
        }
    }


    private fun onChangeJvmArguments(project: ProjectDetail, newArguments: String) {
//        console.log("onDelete: name - $name")
        async {
            clientRestApi.changeJvmArgumentsForProject(project.name, newArguments)
            props.didChangeJvmArgs?.invoke()
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun ChildrenBuilder.render() {
        h2 {
            css {
                marginTop = 0.px
            }
            +"Available Projects"
        }

        if (state.starting) {
            span {
                css {
                    float = Float.right
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
                key = project.name

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
