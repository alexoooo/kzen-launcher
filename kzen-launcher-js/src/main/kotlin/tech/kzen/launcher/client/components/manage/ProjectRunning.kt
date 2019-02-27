package tech.kzen.launcher.client.components.manage

import kotlinx.css.em
import kotlinx.css.px
import react.*
import react.dom.*
import styled.css
import styled.styledH2
import styled.styledSpan
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.api.shellRestApi
import tech.kzen.launcher.client.wrap.*


@Suppress("unused")
class ProjectRunning : RComponent<ProjectRunning.Props, RState>() {
    //-----------------------------------------------------------------------------------------------------------------
    class Props(
            var projects: List<String>?,
            var didStop: (() -> Unit)?
    ) : RProps


    //-----------------------------------------------------------------------------------------------------------------
    private fun onStop(name: String) {
//        console.log("onStop: name - $name")
        async {
            shellRestApi.stopProject(name)

            props.didStop?.invoke()
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun RBuilder.render() {
        styledH2 {
            css {
                marginTop = 0.px
            }

            +"Running Projects"
        }

        if (props.projects != null) {
            renderList(props.projects!!)
        }
        else {
            +"Loading..."
        }
    }


    private fun RBuilder.renderList(projects: List<String>) {
        if (projects.isEmpty()) {
            styledSpan {
                css {
                    fontSize = 1.5.em
                }
                +"None, start one in Available Projects (below)"
            }
        }
        else {
            for (project in projects) {
                div {
                    a(href = "/$project/") {
                        +(project)
                    }

                    child(MaterialButton::class) {
                        attrs {
                            variant = "outlined"

                            onClick = {
                                onStop(project)
                            }
                        }

                        child(StopIcon::class) {
                            attrs {
                                style = reactStyle {
                                    marginRight = 0.25.em
                                }
                            }
                        }

                        +"Stop"
                    }
                }
            }
        }
    }
}
