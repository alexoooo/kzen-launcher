package tech.kzen.launcher.client.components

import kotlinx.css.*
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.*
import styled.css
import styled.styledDiv
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.api.clientRestApi
import tech.kzen.launcher.client.wrap.*


@Suppress("unused")
class ProjectCreate(
        props: ProjectCreate.Props
) : RComponent<ProjectCreate.Props, ProjectCreate.State>(props) {
    //-----------------------------------------------------------------------------------------------------------------
    companion object {
        private const val defaultName = "new-project-name"
        private const val defaultPath = "../kzen-proj/existing-project-name"
    }


    //-----------------------------------------------------------------------------------------------------------------
    class Props(
            var artifacts: Map<String, String>?,
            var didCreate: (() -> Unit)?
    ) : RProps


    class State(
            var name: String,
            var type: String?,
            var path: String
    ) : RState


    //-----------------------------------------------------------------------------------------------------------------
    override fun State.init(props: Props) {
//        console.log("init: props.projects - ${props.artifacts}")

        name = defaultName
        path = defaultPath
        type = props.artifacts?.keys?.iterator()?.next()
    }



    override fun componentDidUpdate(prevProps: Props, prevState: State, snapshot: Any) {
        if (state.type == null && props.artifacts != null) {
            setState {
                type = props.artifacts!!.keys.iterator().next()
            }
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    private fun onNameChange(projectName: String) {
//        console.log("%%%%5 onNameChange", projectName)

        setState {
            name = projectName
        }
    }


    private fun onTypeChange(projectType: String) {
//        console.log("%%%%5 onTypeChange", projectType)

        setState {
            type = projectType
        }
    }


    private fun onCreate() {
//        console.log("onSubmit: props - ${state.name} | ${state.type}")

        async {
            check(state.type != null) {"Type missing"}
            clientRestApi.createProject(state.name, state.type!!)

            setState {
                name = defaultName
                type = null
            }

            props.didCreate?.invoke()
        }
    }


    private fun onPathChange(projectPath: String) {
        setState {
            path = projectPath
        }
    }


    private fun onImport() {
        async {
            check(state.path.isNotBlank()) {"Path missing"}
            clientRestApi.importProject(state.path)

            setState {
                path = defaultPath
            }

            props.didCreate?.invoke()
        }
    }



    //-----------------------------------------------------------------------------------------------------------------
    override fun RBuilder.render() {
        console.log("render: ${props.artifacts} | ${state.name} | ${state.type}")

        h2 {
            +"New Project"
        }

        styledDiv {
            css {
//                backgroundColor = Color.lightGoldenrodYellow
            }

            styledDiv {
                css {
//                    backgroundColor = Color.lightCyan

                    display = Display.inlineBlock
                }

                styledDiv {
                    renderName()
                }

                styledDiv {
                    css {
                        marginTop = 1.em
                        marginBottom = 1.em
                    }

                    renderTypeSelect()
                }

                div {
                    child(MaterialButton::class) {
                        attrs {
                            variant = "outlined"
                            onClick = ::onCreate
                        }

                        +"Create"
                    }
                }
            }


            styledDiv {
                css {
//                    backgroundColor = Color.lightSteelBlue

                    marginLeft = 2.em
                    marginRight = 2.em
                    display = Display.inlineBlock

                    borderLeftWidth = 1.px
                    borderLeftStyle = BorderStyle.solid
                    borderLeftColor = Color.lightGray

                    marginTop = 4.em
                    verticalAlign = VerticalAlign.top

                    height = 3.em
                }
                +" "
            }


            styledDiv {
                css {
//                    backgroundColor = Color.lightSalmon

                    display = Display.inlineBlock
                }

                styledDiv {
                    css {
                        marginTop = 1.5.em
                        marginBottom = 1.em
                    }

                    renderPath()
                }

                div {
                    child(MaterialButton::class) {
                        attrs {
                            variant = "outlined"
                            onClick = ::onImport
                        }

                        +"Import"
                    }
                }
            }
        }
    }


    private fun RBuilder.renderName() {
        child(MaterialTextField::class) {
            attrs {
                style = reactStyle {
                    width = 24.em
                }

//                fullWidth = true

                label = "Name"
                value = state.name

                onChange = {
                    val target = it.target as HTMLInputElement
                    onNameChange(target.value)
                }
            }
        }
    }


    private fun RBuilder.renderTypeSelect() {
//        +"asdasd:"
//
//        br {}

        if (props.artifacts == null || state.type == null) {
            +"Loading..."
        }
        else {

//            console.log("######## state.type: ${state.type}")
            styledDiv {
                css {
                    width = 24.em
                }
//                console.log("^^^^^ ReactSelect::class - default", ReactSelect::class)

                val selectOptions = props
                        .artifacts!!
                        .keys
                        .map { ReactSelectOption(it, it) }
                        .toTypedArray()

                val selectId = "material-react-select-id"

                child(MaterialInputLabel::class) {
                    attrs {
                        htmlFor = selectId

                        style = reactStyle {
                            fontSize = 0.8.em
                        }
                    }
                    +"Type"
                }

                child(ReactSelect::class) {
                    attrs {
                        id = selectId
                        value = selectOptions.find { it.value == state.type }

                        options = selectOptions

                        onChange = {
//                            console.log("CHANGED!!!! -", it)
                            onTypeChange(it.value)
                        }

//                        components = json(
//                                "Control" to ::materialReactSelectController)
                    }
                }
            }
        }
    }


    private fun RBuilder.renderPath() {
        child(MaterialTextField::class) {
            attrs {
                style = reactStyle {
                    width = 36.em
                }

                label = "Path"
                value = state.path

                onChange = {
                    val target = it.target as HTMLInputElement
                    onPathChange(target.value)
                }
            }
        }
    }
}
