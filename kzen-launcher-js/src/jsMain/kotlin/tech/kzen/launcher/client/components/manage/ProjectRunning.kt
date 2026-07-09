package tech.kzen.launcher.client.components.manage

import emotion.react.css
import mui.material.Button
import mui.material.ButtonVariant
import mui.system.sx
import react.ChildrenBuilder
import react.Key
import react.Props
import react.State
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.span
import tech.kzen.launcher.client.api.launchUiAction
import tech.kzen.launcher.client.api.shellRestApi
import tech.kzen.launcher.client.components.buttonIcon
import tech.kzen.launcher.client.components.sectionHeading
import tech.kzen.launcher.client.state.LauncherStore
import tech.kzen.launcher.client.wrap.RComponent
import tech.kzen.launcher.client.wrap.StopIcon
import web.cssom.em


//---------------------------------------------------------------------------------------------------------------------
external interface ProjectRunningProps: Props {
    var projects: List<String>?
}


//---------------------------------------------------------------------------------------------------------------------
@Suppress("unused")
class ProjectRunning(
    props: ProjectRunningProps
): RComponent<ProjectRunningProps, State>(props) {
    //-----------------------------------------------------------------------------------------------------------------
    private fun onStop(name: String) {
        launchUiAction {
            shellRestApi.stopProject(name)

            LauncherStore.invalidateRunning()
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun ChildrenBuilder.render() {
        sectionHeading("Running Projects")

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
                    key = Key(project)

                    a {
                        href = "/$project/"
                        +(project)
                    }

                    Button {
                        variant = ButtonVariant.outlined

                        sx {
                            marginLeft = 1.em
                        }

                        onClick = {
                            onStop(project)
                        }

                        buttonIcon(StopIcon::class)

                        +"Stop"
                    }
                }
            }
        }
    }
}
