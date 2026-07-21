package tech.kzen.launcher.client.components.manage

import emotion.react.css
import mui.material.Button
import mui.material.ButtonVariant
import mui.material.Chip
import mui.material.ChipColor
import mui.material.ChipVariant
import mui.material.CircularProgress
import mui.material.Divider
import mui.material.Size
import mui.system.sx
import react.ChildrenBuilder
import react.Component
import react.Key
import react.Props
import react.ReactNode
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
import tech.kzen.launcher.client.wrap.PlayArrowIcon
import tech.kzen.launcher.client.wrap.RComponent
import tech.kzen.launcher.client.wrap.RemoveCircleOutlinedIcon
import tech.kzen.launcher.client.wrap.StopIcon
import tech.kzen.launcher.common.dto.RunningProject
import tech.kzen.launcher.common.dto.RunningState
import web.cssom.AlignItems
import web.cssom.Auto
import web.cssom.Color
import web.cssom.Display
import web.cssom.FontFamily
import web.cssom.FontWeight
import web.cssom.Padding
import web.cssom.WhiteSpace
import web.cssom.em
import web.cssom.number
import web.cssom.px
import kotlin.reflect.KClass


//---------------------------------------------------------------------------------------------------------------------
external interface ProjectRunningProps: Props {
    var projects: List<RunningProject>?

    // Names the Available section still holds: a project deleted while it was running can be dismissed
    //  but not restarted.
    var restartableNames: Set<String>?
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
            return
        }

        for ((index, project) in projects.withIndex()) {
            div {
                key = Key(project.name)

                // A separator between rows (not above the first) — the newest-started project is on top.
                if (index > 0) {
                    Divider {}
                }

                renderProjectRow(project)
                renderRecentOutput(project)
            }
        }
    }


    // One project as a flex row: name/link on the left, then a status chip (with a spinner while it is
    //  transitioning) and an action, pushed to the right. Running = link + Stop; failed = Dismiss;
    //  exited = Restart + Dismiss.
    private fun ChildrenBuilder.renderProjectRow(project: RunningProject) {
        div {
            css {
                display = Display.flex
                alignItems = AlignItems.center
                padding = Padding(0.5.em, 0.25.em)
                borderRadius = 6.px

                hover {
                    backgroundColor = Color("rgba(0, 0, 0, 0.03)")
                }
            }

            div {
                css {
                    flexGrow = number(1.0)
                    fontWeight = FontWeight.bold
                }
                renderName(project)
            }

            if (project.state == RunningState.STARTING || project.state == RunningState.STOPPING) {
                CircularProgress {
                    sx {
                        width = 1.em
                        height = 1.em
                        marginLeft = 0.5.em
                    }
                }
            }

            renderStatusChip(project)

            when (project.state) {
                RunningState.RUNNING ->
                    renderActionButton("Stop", StopIcon::class) {
                        onStop(project.name)
                    }

                RunningState.FAILED ->
                    renderDismissButton(project.name)

                RunningState.EXITED -> {
                    renderRestartButton(project.name)
                    renderDismissButton(project.name)
                }

                RunningState.STARTING,
                RunningState.STOPPING -> {
                    // Transitional: no action, just the chip + spinner above.
                }
            }
        }
    }


    private fun ChildrenBuilder.renderRestartButton(name: String) {
        if (props.restartableNames?.contains(name) != true) {
            return
        }

        renderActionButton("Restart", PlayArrowIcon::class) {
            LauncherStore.restartProject(name)
        }
    }


    private fun ChildrenBuilder.renderDismissButton(name: String) {
        renderActionButton("Dismiss", RemoveCircleOutlinedIcon::class) {
            onStop(name)
        }
    }


    // The dead child's last output, always shown: a failed or exited row is exactly when the user needs it.
    private fun ChildrenBuilder.renderRecentOutput(project: RunningProject) {
        val recentOutput = project.recentOutput
        if (recentOutput.isNullOrEmpty()) {
            return
        }

        div {
            css {
                fontFamily = FontFamily.monospace
                fontSize = 0.8.em
                whiteSpace = WhiteSpace.preWrap
                maxHeight = 10.em
                overflowY = Auto.auto
                padding = Padding(0.5.em, 0.75.em)
                marginBottom = 0.5.em
                backgroundColor = Color("rgba(0, 0, 0, 0.06)")
                borderRadius = 6.px
            }

            +recentOutput.joinToString("\n")
        }
    }


    private fun ChildrenBuilder.renderName(project: RunningProject) {
        // Only a running child is reverse-proxyable, so only it is a link.
        if (project.state == RunningState.RUNNING) {
            a {
                href = "/${project.name}/"
                +project.name
            }
        }
        else {
            +project.name
        }
    }


    private fun ChildrenBuilder.renderStatusChip(project: RunningProject) {
        Chip {
            size = Size.small
            variant = ChipVariant.filled
            label = ReactNode(statusLabel(project))
            color = statusColor(project.state)

            sx {
                marginLeft = 0.5.em
            }
        }
    }


    private fun statusLabel(project: RunningProject): String {
        return when (project.state) {
            RunningState.STARTING -> "starting"
            RunningState.RUNNING -> "running"
            RunningState.STOPPING -> "stopping"
            RunningState.FAILED -> "failed"
            RunningState.EXITED -> project.exitCode?.let { "exited ($it)" } ?: "exited"
        }
    }


    private fun statusColor(state: RunningState): ChipColor {
        return when (state) {
            RunningState.STARTING -> ChipColor.info
            RunningState.RUNNING -> ChipColor.success
            RunningState.STOPPING -> ChipColor.warning
            RunningState.FAILED,
            RunningState.EXITED -> ChipColor.error
        }
    }


    private fun ChildrenBuilder.renderActionButton(
        label: String,
        icon: KClass<out Component<IconProps, *>>,
        onAction: () -> Unit
    ) {
        Button {
            variant = ButtonVariant.outlined

            sx {
                marginLeft = 1.em
            }

            onClick = {
                onAction()
            }

            buttonIcon(icon)

            +label
        }
    }
}
