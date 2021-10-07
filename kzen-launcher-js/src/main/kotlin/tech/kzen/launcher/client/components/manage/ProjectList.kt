package tech.kzen.launcher.client.components.manage


import kotlinx.coroutines.delay
import kotlinx.css.*
import react.*
import react.dom.div
import styled.css
import styled.styledH2
import styled.styledSpan
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.api.clientRestApi
import tech.kzen.launcher.client.api.shellRestApi
import tech.kzen.launcher.client.wrap.MaterialCircularProgress
import tech.kzen.launcher.client.wrap.MaterialDivider
import tech.kzen.launcher.common.dto.ProjectDetail


@Suppress("unused")
class ProjectList(
        props: Props
): RComponent<ProjectList.Props, ProjectList.State>(props) {
    //-----------------------------------------------------------------------------------------------------------------
    interface Props: react.Props {
        var projects: List<ProjectDetail>?

        var didStart: (() -> Unit)?
        var didRemove: (() -> Unit)?
        var didDelete: (() -> Unit)?
        var didRename: (() -> Unit)?
        var didChangeJvmArgs: (() -> Unit)?
    }


    interface State: react.State {
        var starting: Boolean
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun State.init(props: Props) {
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
    override fun RBuilder.render() {
        styledH2 {
            css {
                marginTop = 0.px
            }
            +"Available Projects"
        }

        if (state.starting) {
            styledSpan {
                css {
                    float = Float.right
                    marginTop = (-55).px
                }
                child(MaterialCircularProgress::class) {}
            }
        }

        val projects = props.projects
        if (projects != null) {
            if (projects.isEmpty()) {
                styledSpan {
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


    private fun RBuilder.renderProjects(projects: List<ProjectDetail>) {
        for (project in projects) {
            div {
                key = project.name

                child(MaterialDivider::class) {}

                child(ProjectItem::class) {
                    attrs {
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
}
