package tech.kzen.launcher.client.components.manage

import csstype.em
import csstype.px
import emotion.react.css
import js.core.jso
import mui.material.Button
import mui.material.ButtonVariant
import mui.material.HiddenImplementation.Companion.css
import react.*
import react.dom.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.span
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.api.shellRestApi
import tech.kzen.launcher.client.wrap.*


//---------------------------------------------------------------------------------------------------------------------
external interface ProjectRunningProps: Props {
    var projects: List<String>?
    var didStop: (() -> Unit)?
}


//---------------------------------------------------------------------------------------------------------------------
@Suppress("unused")
class ProjectRunning(
        props: ProjectRunningProps
): RComponent<ProjectRunningProps, State>(props) {
    //-----------------------------------------------------------------------------------------------------------------
    private fun onStop(name: String) {
        async {
            shellRestApi.stopProject(name)

            props.didStop?.invoke()
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun ChildrenBuilder.render() {
        h2 {
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


    private fun ChildrenBuilder.renderList(projects: List<String>) {
        if (projects.isEmpty()) {
            span {
                css {
                    fontSize = 1.5.em
                }
                +"None, start one in Available Projects (below)"
            }
        }
        else {
            for (project in projects) {
                div {
                    a {
                        href = "/$project/"
                        +(project)
                    }

                    Button {
                        variant = ButtonVariant.outlined

                        css {
                            marginLeft = 1.em
                        }

                        onClick = {
                            onStop(project)
                        }

                        StopIcon::class.react {
                            style = jso {
                                marginRight = 0.25.em
                            }
                        }

                        +"Stop"
                    }
                }
            }
        }
    }
}
