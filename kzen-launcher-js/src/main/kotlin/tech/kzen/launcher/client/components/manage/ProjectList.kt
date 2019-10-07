package tech.kzen.launcher.client.components.manage


import kotlinx.css.*
import kotlinx.coroutines.delay
import kotlinx.html.InputType
import kotlinx.html.js.onClickFunction
import kotlinx.html.title
import react.*
import react.dom.*
import styled.*
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.api.clientRestApi
import tech.kzen.launcher.client.api.shellRestApi
import tech.kzen.launcher.client.wrap.*
import tech.kzen.launcher.common.dto.ProjectDetail


@Suppress("unused")
class ProjectList(
        props: Props
): RComponent<ProjectList.Props, ProjectList.State>(props) {
    //-----------------------------------------------------------------------------------------------------------------
    class Props(
            var projects: List<ProjectDetail>?,

            var didStart: (() -> Unit)?,
            var didRemove: (() -> Unit)?,
            var didDelete: (() -> Unit)?,
            var didRename: (() -> Unit)?
    ): RProps


    class State(
            var starting: Boolean = false
    ): RState


    //-----------------------------------------------------------------------------------------------------------------
    private fun onStart(project: ProjectDetail) {
        setState {
            starting = true
        }

        async {
            delay(1)

            try {
                shellRestApi.startProject(project.name, project.path)
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
                    }
                }
            }
        }
    }
}
