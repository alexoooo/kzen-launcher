package tech.kzen.launcher.client.components.manage

import emotion.react.css
import js.objects.jso
import mui.material.Button
import mui.material.ButtonVariant
import mui.material.TextField
import mui.system.sx
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.span
import react.dom.onChange
import tech.kzen.launcher.client.wrap.*
import tech.kzen.launcher.common.dto.ProjectDetail
import web.cssom.*
import web.html.HTMLInputElement
import kotlin.reflect.KClass


//---------------------------------------------------------------------------------------------------------------------
external interface ProjectItemProps: Props {
    var project: ProjectDetail
    var starting: Boolean

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


    override fun componentDidUpdate(prevProps: ProjectItemProps, prevState: ProjectItemState, snapshot: Any) {
//        if (state.renaming && != prevState.renaming) {
//            setState {
//                type = props.artifacts!!.iterator().next().name
//            }
//        }
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

                if (! props.project.exists) {
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

                if (! props.project.exists) {
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
                    renderRename()
                    renderChangeArgs()
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

                if (props.starting) {
                    disabled = true
                }

                PlayArrowIcon::class.react {
                    style = jso {
                        marginRight = 0.25.em
                    }
                }

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

                DeleteIcon::class.react {
                    style = jso {
                        marginRight = 0.25.em
                    }
                }

                +"Delete"
            }
        }
    }


    private fun ChildrenBuilder.renderRenameTitle() {
        TextField  {
            sx {
                width = 36.em
            }

            label = ReactNode("New name")
            value = state.newName

            onChange = {
                val target = it.target as HTMLInputElement
                onRenameChange(target.value)
            }
        }
    }


    private fun ChildrenBuilder.renderRename() {
        div {
            css {
                display = Display.inlineBlock
                marginLeft = 1.em
            }

            Button {
                variant = ButtonVariant.outlined

                onClick = {
                    if (state.renaming) {
                        onRenameCommit()
                    }
                    else {
                        onRenameStart()
                    }
                }

                val icon: KClass<out Component<IconProps, State>> =
                        if (state.renaming) {
                            SaveIcon::class
                        }
                        else {
                            EditIcon::class
                        }

                icon.react {
                    style = jso {
                        marginRight = 0.25.em
                    }
                }

                +"Rename"
            }
        }
    }


    private fun ChildrenBuilder.renderChangeArgs() {
        div {
            css {
                display = Display.inlineBlock
                marginLeft = 1.em
            }

            Button {
                variant = ButtonVariant.outlined

                onClick = {
                    if (state.changingArgs) {
                        onChangeArgsCommit()
                    }
                    else {
                        onChangeArgsStart()
                    }
                }

                val icon: KClass<out Component<IconProps, State>> =
                        if (state.changingArgs) {
                            SaveIcon::class
                        }
                        else {
                            EditIcon::class
                        }

                icon.react {
                    style = jso {
                        marginRight = 0.25.em
                    }
                }

                +"JVM Arguments"
            }
        }
    }


    private fun ChildrenBuilder.renderJvmArgs() {
        if (! state.changingArgs && props.project.jvmArgs.isEmpty()) {
            return
        }

        div {
            if (state.changingArgs) {
                TextField {
                    sx {
                        width = 36.em
                    }

                    label = ReactNode("New JVM Arguments")
                    value = state.newJvmArgs

                    onChange = {
                        val target = it.target as HTMLInputElement
                        onChangeArgs(target.value)
                    }
                }
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

                RemoveCircleOutlineIcon::class.react {
                    style = jso {
                        marginRight = 0.25.em
                    }
                }

                +"Remove"
            }
        }
    }
}