package tech.kzen.launcher.client.components.manage

import kotlinx.css.*
import org.w3c.dom.HTMLInputElement
import react.*
import styled.css
import styled.styledDiv
import styled.styledSpan
import tech.kzen.launcher.client.wrap.*
import tech.kzen.launcher.common.dto.ProjectDetail
import kotlin.reflect.KClass


class ProjectItem(
        props: Props
): RComponent<ProjectItem.Props, ProjectItem.State>(props) {
    //-----------------------------------------------------------------------------------------------------------------
    class Props(
            var project: ProjectDetail,
            var starting: Boolean,

            var onStart: ((ProjectDetail) -> Unit),
            var onRemove: ((ProjectDetail) -> Unit),
            var onDelete: ((ProjectDetail) -> Unit),
            var onRename: ((ProjectDetail, String) -> Unit)
    ): RProps


    class State(
            var renaming: Boolean,
            var newName: String
    ): RState


    //-----------------------------------------------------------------------------------------------------------------
    override fun State.init(props: Props) {
        renaming = false
        newName = props.project.name
    }


    override fun componentDidUpdate(prevProps: Props, prevState: State, snapshot: Any) {
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


    //-----------------------------------------------------------------------------------------------------------------
    override fun RBuilder.render() {
        styledDiv {
            css {
                marginBottom = 1.em

                if (! props.project.exists) {
                    // TODO: should not apply to 'remove' button
                    opacity = 0.5
                }
            }

            styledDiv {
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

            styledSpan {
                css {
                    fontFamily = "monospace"
                }

                +(props.project.path)
            }

            styledDiv {
                if (props.project.exists) {
                    renderRun()
                    renderDelete()
                    renderRename()
                }
                else {
                    renderRemove()
                }
            }
        }
    }


    private fun RBuilder.renderRun() {
        styledDiv {
            css {
                display = Display.inlineBlock
            }

            child(MaterialButton::class) {
                attrs {
                    variant = "outlined"
                    onClick = ::onStart

                    if (props.starting) {
                        disabled = true
                    }
                }

                child(PlayArrowIcon::class) {
                    attrs {
                        style = reactStyle {
                            marginRight = 0.25.em
                        }
                    }
                }

                +"Run"
            }
        }
    }


    private fun RBuilder.renderDelete() {
        styledDiv {
            css {
                display = Display.inlineBlock
                marginLeft = 1.em
            }

            child(MaterialButton::class) {
                attrs {
                    variant = "outlined"
                    onClick = ::onDelete
                }

                child(DeleteIcon::class) {
                    attrs {
                        style = reactStyle {
                            marginRight = 0.25.em
                        }
                    }
                }

                +"Delete"
            }
        }
    }


    private fun RBuilder.renderRenameTitle() {
        child(MaterialTextField::class) {
            attrs {
                style = reactStyle {
                    width = 36.em
                }

                label = "New name"
                value = state.newName

                onChange = {
                    val target = it.target as HTMLInputElement
                    onRenameChange(target.value)
                }
            }
        }
    }


    private fun RBuilder.renderRename() {
        styledDiv {
            css {
                display = Display.inlineBlock
                marginLeft = 1.em
            }

            child(MaterialButton::class) {
                attrs {
                    variant = "outlined"
                    onClick = {
                        if (state.renaming) {
                            onRenameCommit()
                        }
                        else {
                            onRenameStart()
                        }
                    }
                }

                val icon: KClass<out Component<IconProps, RState>> =
                        if (state.renaming) {
                            SaveIcon::class
                        }
                        else {
                            EditIcon::class
                        }

                child(icon) {
                    attrs {
                        style = reactStyle {
                            marginRight = 0.25.em
                        }
                    }
                }

                +"Rename"
            }
        }
    }


    private fun RBuilder.renderRemove() {
        styledDiv {
            css {
                display = Display.inlineBlock
            }

            child(MaterialButton::class) {
                attrs {
                    variant = "outlined"
                    onClick = ::onRemove
                }

                child(RemoveCircleOutlineIcon::class) {
                    attrs {
                        style = reactStyle {
                            marginRight = 0.25.em
                        }
                    }
                }

                +"Remove"
            }
        }
    }
}