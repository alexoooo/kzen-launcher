package tech.kzen.launcher.client.components.manage

import emotion.react.css
import mui.material.Button
import mui.material.ButtonVariant
import mui.system.sx
import react.ChildrenBuilder
import react.Component
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
import tech.kzen.launcher.client.wrap.IconProps
import tech.kzen.launcher.client.wrap.RComponent
import tech.kzen.launcher.client.wrap.RemoveCircleOutlinedIcon
import tech.kzen.launcher.client.wrap.StopIcon
import tech.kzen.launcher.common.dto.RunningProject
import tech.kzen.launcher.common.dto.RunningState
import web.cssom.Color
import web.cssom.FontWeight
import web.cssom.NamedColor
import web.cssom.em
import kotlin.reflect.KClass


//---------------------------------------------------------------------------------------------------------------------
external interface ProjectRunningProps: Props {
    var projects: List<RunningProject>?
}


//---------------------------------------------------------------------------------------------------------------------
@Suppress("unused")
class ProjectRunning(
    props: ProjectRunningProps
): RComponent<ProjectRunningProps, State>(props) {
    //-----------------------------------------------------------------------------------------------------------------
    // Also serves as "dismiss" for a failed job — the shell/simulator removes a failed entry on stop.
    private fun onStop(name: String) {
        launchUiAction {
            shellRestApi.stopProject(name)

            LauncherStore.invalidateRunning()
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun ChildrenBuilder.render() {
        sectionHeading("Running Projects")

        val projects = props.projects
        if (projects != null) {
            renderList(projects)
        }
        else {
            +"Loading..."
        }
    }


    private fun ChildrenBuilder.renderList(projects: List<RunningProject>) {
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
                    key = Key(project.name)

                    renderProject(project)
                }
            }
        }
    }


    private fun ChildrenBuilder.renderProject(project: RunningProject) {
        when (project.state) {
            RunningState.RUNNING -> {
                a {
                    href = "/${project.name}/"
                    +project.name
                }
                renderActionButton(project.name, "Stop", StopIcon::class)
            }

            RunningState.STARTING ->
                renderTransitionLabel(project.name, "starting…", NamedColor.gray)

            RunningState.STOPPING ->
                renderTransitionLabel(project.name, "stopping…", NamedColor.gray)

            RunningState.FAILED -> {
                renderTransitionLabel(project.name, "failed", NamedColor.darkred)
                renderActionButton(project.name, "Dismiss", RemoveCircleOutlinedIcon::class)
            }
        }
    }


    // A non-interactive row shown while a project is transitioning (or has failed): the name plus a
    //  state suffix, no link (the child is not proxyable in these states).
    private fun ChildrenBuilder.renderTransitionLabel(name: String, state: String, color: Color) {
        span {
            css {
                fontWeight = FontWeight.bold
            }
            +name
        }
        span {
            css {
                marginLeft = 0.5.em
                this.color = color
            }
            +"($state)"
        }
    }


    private fun ChildrenBuilder.renderActionButton(
        name: String,
        label: String,
        icon: KClass<out Component<IconProps, *>>
    ) {
        Button {
            variant = ButtonVariant.outlined

            sx {
                marginLeft = 1.em
            }

            onClick = {
                onStop(name)
            }

            buttonIcon(icon)

            +label
        }
    }
}
