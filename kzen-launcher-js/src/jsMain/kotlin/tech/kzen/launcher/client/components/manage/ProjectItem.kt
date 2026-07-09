package tech.kzen.launcher.client.components.manage

import emotion.react.css
import mui.material.Button
import mui.material.ButtonVariant
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.span
import tech.kzen.launcher.client.components.buttonIcon
import tech.kzen.launcher.client.components.wideTextField
import tech.kzen.launcher.client.wrap.*
import tech.kzen.launcher.common.dto.ProjectDetail
import web.cssom.*


//---------------------------------------------------------------------------------------------------------------------
external interface ProjectItemProps: Props {
    var project: ProjectDetail

    var onStart: ((ProjectDetail) -> Unit)
    var onRemove: ((ProjectDetail) -> Unit)
    var onDelete: ((ProjectDetail) -> Unit)
    var onRename: ((ProjectDetail, String) -> Unit)
    var onChangeJvmArgs: ((ProjectDetail, String) -> Unit)
}


external interface ProjectItemState: State {
    var renaming: Boolean
    var changingArgs: Boolean
    var newName: String
    var newJvmArgs: String
}


//---------------------------------------------------------------------------------------------------------------------
class ProjectItem(
        props: ProjectItemProps
): RComponent<ProjectItemProps, ProjectItemState>(props) {
    //-----------------------------------------------------------------------------------------------------------------
    override fun ProjectItemState.init(props: ProjectItemProps) {
        renaming = false
        newName = props.project.name

        changingArgs = false
        newJvmArgs = props.project.jvmArgs
    }


    //-----------------------------------------------------------------------------------------------------------------
    private fun onStart() {
        props.onStart(props.project)
    }


    private fun onRemove() {
        props.onRemove(props.project)
    }


    private fun onDelete() {
        props.onDelete(props.project)
    }


    private fun onRenameStart() {
        setState {
            renaming = true
        }
    }


    private fun onRenameChange(newName: String) {
        setState {
            this.newName = newName
        }
    }


    private fun onRenameCommit() {
        if (state.newName == props.project.name) {
            setState {
                renaming = false
            }
        }
        else {
            props.onRename(props.project, state.newName)
        }
    }


    private fun onChangeArgsStart() {
        setState {
            changingArgs = true
        }
    }


    private fun onChangeArgs(newJvmArgs: String) {
        setState {
            this.newJvmArgs = newJvmArgs
        }
    }


    private fun onChangeArgsCommit() {
        if (state.newJvmArgs == props.project.jvmArgs) {
            setState {
                changingArgs = false
            }
        }
        else {
            props.onChangeJvmArgs(props.project, state.newJvmArgs)
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun ChildrenBuilder.render() {
        div {
            css {
                marginBottom = 1.em

                if (!props.project.exists) {
                    // TODO: should not apply to 'remove' button
                    opacity = number(0.5)
                }
            }

            div {
                css {
                    fontWeight = FontWeight.bold
                }

                if (state.renaming) {
                    renderRenameTitle()
                }
                else {
                    +(props.project.name)
                }

                if (!props.project.exists) {
                    +" (missing)"
                }
            }

            span {
                css {
                    fontFamily = FontFamily.monospace
                }

                +(props.project.path)
            }

            div {
                if (props.project.exists) {
                    renderRun()
                    renderDelete()
                    renderEditToggleButton(
                        editing = state.renaming,
                        label = "Rename",
                        onStartEdit = ::onRenameStart,
                        onCommit = ::onRenameCommit)
                    renderEditToggleButton(
                        editing = state.changingArgs,
                        label = "JVM Arguments",
                        onStartEdit = ::onChangeArgsStart,
                        onCommit = ::onChangeArgsCommit)
                }
                else {
                    renderRemove()
                }
            }

            renderJvmArgs()
        }
    }


    private fun ChildrenBuilder.renderRun() {
        div {
            css {
                display = Display.inlineBlock
            }

            Button {
                variant = ButtonVariant.outlined
                onClick = { onStart() }

                buttonIcon(PlayArrowIcon::class)

                +"Run"
            }
        }
    }


    private fun ChildrenBuilder.renderDelete() {
        div {
            css {
                display = Display.inlineBlock
                marginLeft = 1.em
            }

            Button {
                variant = ButtonVariant.outlined
                onClick = { onDelete() }

                buttonIcon(DeleteIcon::class)

                +"Delete"
            }
        }
    }


    private fun ChildrenBuilder.renderRenameTitle() {
        wideTextField("New name", state.newName, ::onRenameChange)
    }


    // Shared by the Rename and JVM-Arguments controls: an outlined button that toggles between an
    // Edit (start editing) and Save (commit) affordance based on whether that field is currently editing.
    private fun ChildrenBuilder.renderEditToggleButton(
        editing: Boolean,
        label: String,
        onStartEdit: () -> Unit,
        onCommit: () -> Unit
    ) {
        div {
            css {
                display = Display.inlineBlock
                marginLeft = 1.em
            }

            Button {
                variant = ButtonVariant.outlined

                onClick = {
                    if (editing) {
                        onCommit()
                    }
                    else {
                        onStartEdit()
                    }
                }

                buttonIcon(if (editing) SaveIcon::class else EditIcon::class)

                +label
            }
        }
    }


    private fun ChildrenBuilder.renderJvmArgs() {
        if (!state.changingArgs && props.project.jvmArgs.isEmpty()) {
            return
        }

        div {
            if (state.changingArgs) {
                wideTextField("New JVM Arguments", state.newJvmArgs, ::onChangeArgs)
            }
            else {
                css {
                    fontFamily = FontFamily.monospace
                }

                +"JVM Arguments: ${props.project.jvmArgs}"
            }
        }
    }


    private fun ChildrenBuilder.renderRemove() {
        div {
            css {
                display = Display.inlineBlock
            }

            Button {
                variant = ButtonVariant.outlined
                onClick = { onRemove() }

                buttonIcon(RemoveCircleOutlinedIcon::class)

                +"Remove"
            }
        }
    }
}