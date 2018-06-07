package tech.kzen.launcher.client.components


import kotlinx.html.InputType
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.*
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.api.shellRestApi


@Suppress("unused")
class ProjectRunning : RComponent<ProjectRunning.Props, RState>() {
    //-----------------------------------------------------------------------------------------------------------------
    class Props(var projects: List<String>) : RProps


    //-----------------------------------------------------------------------------------------------------------------
    private fun onStop(name: String) {
        console.log("onStop: name - $name")
        async {
            shellRestApi.stopProject(name)
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun RBuilder.render() {
        fieldSet {
            legend {
                +"Running projects"
            }

            for (project in props.projects) {
                div {
                    a(href = "/$project/") {
                        +(project)
                    }

                    input (type = InputType.button) {
                        attrs {
                            value = "Stop"
                            onClickFunction = {
                                onStop(project)
                            }
                        }
                    }
                }
            }
        }
    }
}
