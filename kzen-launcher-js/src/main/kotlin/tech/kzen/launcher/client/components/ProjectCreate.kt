package tech.kzen.launcher.client.components

import kotlinx.css.Color
import kotlinx.css.Display
import kotlinx.css.Float
import kotlinx.css.LinearDimension
import kotlinx.css.em
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.*
import styled.css
import styled.styledDiv
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.api.clientRestApi
import tech.kzen.launcher.client.wrap.*
import kotlin.js.json


@Suppress("unused")
class ProjectCreate(
        props: ProjectCreate.Props
) : RComponent<ProjectCreate.Props, ProjectCreate.State>(props) {
    //-----------------------------------------------------------------------------------------------------------------
    companion object {
        private const val defaultName = "new-project-name"
    }


    //-----------------------------------------------------------------------------------------------------------------
    class Props(
            var artifacts: Map<String, String>?,
            var didCreate: (() -> Unit)?
    ) : RProps


    class State(
            var name: String,
            var type: String?
    ) : RState


    //-----------------------------------------------------------------------------------------------------------------
    override fun State.init(props: Props) {
        console.log("init: props.projects - ${props.artifacts}")

        name = defaultName
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
        console.log("%%%%5 onTypeChange", projectType)

        setState {
            type = projectType
        }
    }


    private fun onSubmit() {
        console.log("onSubmit: props - ${state.name} | ${state.type}")

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



    //-----------------------------------------------------------------------------------------------------------------
    override fun RBuilder.render() {
        console.log("render: ${props.artifacts} | ${state.name} | ${state.type}")

        h2 {
            +"Create New Project"
        }

        div {
            styledDiv {
                css {
                    display = Display.inlineBlock
                }

                renderName()
            }

            styledDiv {
                css {
                    display = Display.inlineBlock
                    marginLeft = 1.em
                }

                renderTypeSelect()
            }
        }

        div {
            input (type = InputType.button) {
                attrs {
                    value = "Create"

                    onClickFunction = { onSubmit() }
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

//            div {
//
//                div {
//                    attrs {
//                        id = "foo-bar"
//                    }
//                    +"foo bar"
//                }
//            }


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

                child(MaterialInputLabel::class) {
                    attrs {
                        htmlFor = "foo-bar"

                        style = reactStyle {
                            fontSize = 0.8.em
                        }
                    }
                    +"Type"
                }

                child(ReactSelect::class) {
                    attrs {
                        id = "foo-bar"
                        value = selectOptions.find { it.value == state.type }

                        options = selectOptions

                        onChange = {
//                            console.log("CHANGED!!!! -", it)
                            onTypeChange(it.value)
                        }
                    }
                }
            }
        }
    }
}
