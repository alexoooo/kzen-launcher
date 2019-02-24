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
        props: ProjectList.Props
): RComponent<ProjectList.Props, ProjectList.State>(props) {
    //-----------------------------------------------------------------------------------------------------------------
    class Props(
            var projects: List<ProjectDetail>?,

            var didStart: (() -> Unit)?,
            var didRemove: (() -> Unit)?,
            var didDelete: (() -> Unit)?
    ): RProps


    class State(
            var starting: Boolean = false
    ): RState


    //-----------------------------------------------------------------------------------------------------------------
    private fun onStart(name: String, location: String) {
        console.log("onStart: name - $name | location - $location")

        setState {
            starting = true
        }

        async {
            delay(1)

            try {
                shellRestApi.startProject(name, location)
                props.didStart?.invoke()
            }
            finally {
                setState {
                    starting = false
                }
            }
        }
    }


    private fun onRemove(name: String) {
        console.log("onRemove: name - $name")
        async {
            clientRestApi.removeProject(name)
            props.didRemove?.invoke()
        }
    }


    private fun onDelete(name: String) {
        console.log("onDelete: name - $name")
        async {
            clientRestApi.deleteProject(name)
            props.didDelete?.invoke()
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun RBuilder.render() {
        h2 {
            +"Available Projects"
        }

        if (state.starting) {
            styledSpan {
                css {
                    float = Float.right
                    marginTop = (-65).px
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
                    +"None, please add a New Project (top right)"
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
            child(MaterialDivider::class) {}

            renderProject(project)
        }
    }


    private fun RBuilder.renderProject(project: ProjectDetail) {
        styledDiv {
            attrs {
                key = project.name
            }

            css {
                marginBottom = 1.em

                if (! project.exists) {
                    opacity = 0.5
                }
            }


            styledDiv {
                css {
                    fontWeight = FontWeight.bold
                }

                +(project.name)

                if (! project.exists) {
                    +" (missing)"
                }
            }


            styledSpan {
                css {
                    fontFamily = "monospace"
                }

                +(project.path)
            }


            styledDiv {
                styledDiv {
                    css {
                        display = Display.inlineBlock
//                            position = Position.relative
                    }

                    child(MaterialButton::class) {
                        attrs {
                            variant = "outlined"
                            onClick = {
                                onStart(project.name, project.path)
                            }

                            if (state.starting) {
                                disabled = true
                            }
                        }

                        +"Run"
                    }
                }


                if (project.exists) {
                    styledDiv {
                        css {
                            display = Display.inlineBlock
//                            position = Position.relative
                        }

                        child(MaterialButton::class) {
                            attrs {
                                variant = "outlined"
                                onClick = {
                                    onDelete(project.name)
                                }
                            }

                            +"Delete"
                        }
                    }
                }
                else {
                    styledDiv {
                        css {
                            display = Display.inlineBlock
//                            position = Position.relative
                        }

                        child(MaterialButton::class) {
                            attrs {
                                variant = "outlined"
                                onClick = {
                                    onRemove(project.name)
                                }
                            }

                            +"Remove"
                        }
                    }
                }
            }
        }
    }
}
