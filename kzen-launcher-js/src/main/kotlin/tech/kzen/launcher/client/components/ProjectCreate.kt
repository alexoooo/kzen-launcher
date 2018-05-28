package tech.kzen.launcher.client.components

import kotlinext.js.jsObject
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.*
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.api.restApi


@Suppress("unused")
class ProjectCreate(
        props: ProjectCreate.Props
) : RComponent<ProjectCreate.Props, ProjectCreate.State>(props) {

    //-----------------------------------------------------------------------------------------------------------------
    class Props(
            var projects: Map<String, String>
    ) : RProps


    class State(
            var name: String,
            var type: String
    ) : RState


    //-----------------------------------------------------------------------------------------------------------------
    override fun State.init(props: Props) {
//        console.log("init: props.projects - ${props.projects}")

        name = "New project name"
        type = props.projects.keys.iterator().next()
    }



    //-----------------------------------------------------------------------------------------------------------------
    private fun onNameChange(projectName: String) {
        setState {
            name = projectName
        }
    }

    private fun onTypeChange(projectType: String) {
        setState {
            type = projectType
        }
    }


    private fun onSubmit() {
        console.log("onSubmit: props - ${state.name} | ${state.type}")

        async {
            restApi.createProject(state.name, state.type)
        }
    }



    //-----------------------------------------------------------------------------------------------------------------
    override fun RBuilder.render() {
        fieldSet {
            legend {
                +"Create new project"
            }


            div {
                +"Name:"
                input (type = InputType.text) {
                    attrs {
                        value = state.name

                        onChangeFunction = {
                            val target = it.target as HTMLInputElement
                            onNameChange(target.value)
                        }
                    }
                }
            }


            div {
                +"Type:"
                for (projectType in props.projects.keys) {
                    div {
                        input(type = InputType.radio) {
                            attrs {
                                checked = (state.type == projectType)
                                onChangeFunction = { onTypeChange(projectType) }
                            }
                        }
                        label {
                            attrs {
                                onChangeFunction = { onTypeChange(projectType) }
                            }
                            +projectType
                        }
                    }
                }
            }


            div {
                input (type = InputType.button) {
                    attrs {
//                        value = "Create - ${state.name} - ${state.type}"
                        value = "Create"

                        onClickFunction = { onSubmit() }
                    }
                }
            }
        }
    }
}
