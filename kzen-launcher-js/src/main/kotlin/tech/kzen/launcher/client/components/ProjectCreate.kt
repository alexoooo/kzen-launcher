package tech.kzen.launcher.client.components

import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.*
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.api.clientRestApi


@Suppress("unused")
class ProjectCreate(
        props: ProjectCreate.Props
) : RComponent<ProjectCreate.Props, ProjectCreate.State>(props) {

    //-----------------------------------------------------------------------------------------------------------------
    class Props(
            var artifacts: Map<String, String>
    ) : RProps


    class State(
            var name: String,
            var type: String
    ) : RState


    //-----------------------------------------------------------------------------------------------------------------
    override fun State.init(props: Props) {
        console.log("init: props.projects - ${props.artifacts}")

        name = "new-project-name"
        type = props.artifacts.keys.iterator().next()
    }



    //-----------------------------------------------------------------------------------------------------------------
    private fun onNameChange(projectName: String) {
        console.log("%%%%5 onNameChange", projectName)

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
            clientRestApi.createProject(state.name, state.type)
        }
    }



    //-----------------------------------------------------------------------------------------------------------------
    override fun RBuilder.render() {
        console.log("render: ${props.artifacts} | ${state.name} | ${state.type}")

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

                console.log("######## state.type: ${state.type}")

                br {}
                select {
                    attrs {
                        value = state.type
                        onChangeFunction = {
                            val value: String =
                                    it.target!!.asDynamic().value as? String
                                    ?: throw IllegalStateException("Archetype name string expected")
                            onTypeChange(value)
                        }

                        // TODO: why is this necessary (or error otherwise)
                        multiple = true
                    }
////
                    for (projectType in props.artifacts.keys) {
                        option {
                            attrs {
                                value = projectType
                                onChangeFunction = {
                                    console.log("#!#@! option onChangeFunction", it.currentTarget)
                                }
                            }

                            +projectType
                        }
                    }
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
    }
}
