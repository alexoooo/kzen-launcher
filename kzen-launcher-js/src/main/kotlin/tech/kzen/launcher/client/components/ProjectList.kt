package tech.kzen.launcher.client.components


import kotlinx.css.Color
import kotlinx.css.FontWeight
import kotlinx.css.em
import kotlinx.html.InputType
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.*
import styled.css
import styled.styledDiv
import styled.styledSpan
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.api.clientRestApi
import tech.kzen.launcher.client.api.shellRestApi
import tech.kzen.launcher.client.wrap.*
import tech.kzen.launcher.common.dto.ProjectDetail


@Suppress("unused")
class ProjectList : RComponent<ProjectList.Props, RState>() {
    //-----------------------------------------------------------------------------------------------------------------
    class Props(
            var projects: List<ProjectDetail>?,
            var didStart: (() -> Unit)?,
            var didRemove: (() -> Unit)?,
            var didDelete: (() -> Unit)?
    ) : RProps


    //-----------------------------------------------------------------------------------------------------------------
    private fun onStart(name: String, location: String) {
        console.log("onStart: name - $name | location - $location")
        async {
            shellRestApi.startProject(name, location)
            props.didStart?.invoke()
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

        val projects = props.projects
        if (projects != null) {
            if (projects.isEmpty()) {
                styledSpan {
                    css {
                        fontSize = 1.5.em
                    }
                    +"None, please Create New Project (above)"
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

            styledDiv {
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
                    child(MaterialButton::class) {
                        attrs {
                            variant = "outlined"
                            onClick = {
                                onStart(project.name, project.path)
                            }
                        }

                        +"Run"
                    }


                    if (project.exists) {
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
                    else {
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
