package tech.kzen.launcher.client.components.add

import kotlinx.css.*
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.*
import styled.css
import styled.styledDiv
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.api.clientRestApi
import tech.kzen.launcher.client.wrap.*
import tech.kzen.launcher.common.dto.ArchetypeDetail
import kotlin.js.json


@Suppress("unused")
class NewProjectScreen(
        props: NewProjectScreen.Props
) : RComponent<NewProjectScreen.Props, NewProjectScreen.State>(props) {
    //-----------------------------------------------------------------------------------------------------------------
    companion object {
        private const val defaultName = "new-project-name"
        private const val defaultPath = "../kzen-proj/existing-project-name"
    }


    //-----------------------------------------------------------------------------------------------------------------
    class Props(
            var artifacts: List<ArchetypeDetail>?,
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
        type = props.artifacts?.iterator()?.next()?.name
    }



    override fun componentDidUpdate(prevProps: Props, prevState: State, snapshot: Any) {
        if (state.type == null && props.artifacts != null) {
            setState {
                type = props.artifacts!!.iterator().next().name
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
//        console.log("render: ${props.artifacts} | ${state.name} | ${state.type}")

        child(MaterialCard::class) {
            attrs {
                style = reactStyle {
                    zIndex = -10

                    backgroundColor = Color.white
                    margin(2.em)
                }
            }

            child(MaterialCardContent::class) {
                h2 {
                    +"Create New"
                }

                renderCreate()
            }
        }

        child(MaterialCard::class) {
            attrs {
                style = reactStyle {
                    backgroundColor = Color.white
                    margin(2.em)
                }
            }

            child(MaterialCardContent::class) {
                h2 {
                    +"Import Existing"
                }

                renderImport()
            }
        }
    }


    private fun RBuilder.renderCreate() {
        styledDiv {
            css {
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
    }


    private fun RBuilder.renderImport() {
        styledDiv {
            css {
                display = Display.inlineBlock
            }

            styledDiv {
                css {
//                    marginTop = 1.5.em
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
        if (props.artifacts == null || state.type == null) {
            +"Loading..."
        }
        else {
            styledDiv {
                css {
                    width = 24.em
                    position = Position.relative
                }
                val selectOptions = props
                        .artifacts!!
                        .map { ReactSelectOption(it.name, it.title + " - " + it.description) }
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

//                        menuContainerStyle = json(
//                                "zIndex" to 999
//                        )

                        onChange = {
                            onTypeChange(it.value)
                        }
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
