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
    class Props(
            var projects: List<String>?,
            var didStop: (() -> Unit)?
    ) : RProps


    //-----------------------------------------------------------------------------------------------------------------
    private fun onStop(name: String) {
        console.log("onStop: name - $name")
        async {
            shellRestApi.stopProject(name)

            props.didStop?.invoke()
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun RBuilder.render() {
        fieldSet {
            legend {
                +"Running projects"
            }

            if (props.projects != null) {
                renderList(props.projects!!)
            }
            else {
                +"Loading..."
            }
        }
    }


    private fun RBuilder.renderList(projects: List<String>) {
        for (project in projects) {
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
