package tech.kzen.launcher.client.components.manage

import emotion.react.css
import mui.material.Button
import mui.material.ButtonColor
import mui.material.ButtonVariant
import mui.material.Dialog
import mui.material.DialogActions
import mui.material.DialogContent
import mui.material.DialogContentText
import mui.material.DialogTitle
import mui.system.sx
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.span
import tech.kzen.launcher.client.components.buttonIcon
import tech.kzen.launcher.client.components.buttonSpinner
import tech.kzen.launcher.client.components.wideTextField
import tech.kzen.launcher.client.wrap.*
import tech.kzen.launcher.common.dto.ArchetypeDetail
import tech.kzen.launcher.common.dto.ProjectDetail
import web.cssom.*
import kotlin.js.Promise


//---------------------------------------------------------------------------------------------------------------------
external interface ProjectItemProps: Props {
    var project: ProjectDetail
    var archetypes: List<ArchetypeDetail>?

    var onStart: ((ProjectDetail) -> Unit)
    var onRemove: ((ProjectDetail) -> Unit)
    var onDelete: ((ProjectDetail) -> Promise<Unit>)
    var onUpgrade: ((ProjectDetail, String) -> Promise<Unit>)
    var onRename: ((ProjectDetail, String) -> Unit)
    var onChangeJvmArgs: ((ProjectDetail, String) -> Unit)
}


external interface ProjectItemState: State {
    var renaming: Boolean
    var changingArgs: Boolean
    var newName: String
    var newJvmArgs: String
    var deleting: Boolean
    var confirmingDelete: Boolean
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

        deleting = false
        confirmingDelete = false
    }


    //-----------------------------------------------------------------------------------------------------------------
    private fun onStart() {
        props.onStart(props.project)
    }


    private fun onRemove() {
        props.onRemove(props.project)
    }


    // "Delete and Remove" is destructive — it permanently deletes the project directory from disk — so it
    //  is confirmed first (the plain "Remove" only drops the list entry and needs no confirmation).
    private fun onDeleteRequest() {
        if (state.deleting) {
            return
        }

        setState {
            confirmingDelete = true
        }
    }


    private fun onDeleteCancel() {
        setState {
            confirmingDelete = false
        }
    }


    // Fire the delete and show a spinner on the button until it settles. On success the project drops out
    //  of the list and this row unmounts; on failure it stays (error surfaced by the interceptor) and the
    //  spinner clears so the user can retry.
    private fun onDeleteConfirm() {
        setState {
            confirmingDelete = false
            deleting = true
        }

        props.onDelete(props.project)
            .then { clearDeleting() }
            .catch { clearDeleting() }
    }


    private fun clearDeleting() {
        setState {
            deleting = false
        }
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

            renderVersion()

            div {
                if (props.project.exists) {
                    renderRun()
                    renderRemove()
                    renderDeleteAndRemove()
                    ProjectUpgradeControl::class.react {
                        this.project = props.project
                        this.archetypes = props.archetypes

                        onUpgrade = props.onUpgrade
                    }
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


    // The recorded archetype version, next to the path so the project's provenance is visible. An unknown
    //  version (import / pre-SH4) reads as such — that project is offered every cached version to upgrade to.
    private fun ChildrenBuilder.renderVersion() {
        div {
            css {
                fontSize = 0.85.em
                color = Color("rgba(0, 0, 0, 0.55)")
            }

            if (props.project.version == ProjectDetail.unknownValue) {
                +"Version: unknown"
            }
            else {
                +"Version: ${props.project.version}"
            }
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


    private fun ChildrenBuilder.renderDeleteAndRemove() {
        div {
            css {
                display = Display.inlineBlock
                marginLeft = 1.em
            }

            Button {
                variant = ButtonVariant.outlined
                disabled = state.deleting
                onClick = { onDeleteRequest() }

                if (state.deleting) {
                    buttonSpinner()
                }
                else {
                    buttonIcon(DeleteForeverIcon::class)
                }

                +"Delete and Remove"
            }

            renderDeleteConfirmDialog()
        }
    }


    // Confirmation for the destructive "Delete and Remove". Backdrop click / Escape / Cancel all abort;
    //  only the red confirm button fires the delete (which then shows the button spinner above).
    private fun ChildrenBuilder.renderDeleteConfirmDialog() {
        Dialog {
            open = state.confirmingDelete
            onClose = { _, _ -> onDeleteCancel() }

            DialogTitle {
                +"Delete and remove project?"
            }

            DialogContent {
                DialogContentText {
                    +"This permanently deletes \"${props.project.name}\" and its project directory from disk:"
                }

                DialogContentText {
                    sx {
                        fontFamily = FontFamily.monospace
                        marginTop = 0.5.em
                    }
                    +props.project.path
                }
            }

            DialogActions {
                Button {
                    variant = ButtonVariant.outlined
                    onClick = { onDeleteCancel() }

                    +"Cancel"
                }

                Button {
                    variant = ButtonVariant.contained
                    color = ButtonColor.error
                    onClick = { onDeleteConfirm() }

                    buttonIcon(DeleteForeverIcon::class)

                    +"Delete and Remove"
                }
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
                marginLeft = 1.em
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