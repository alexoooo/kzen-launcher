package tech.kzen.launcher.client.components


import kotlinx.css.Color
import kotlinx.html.InputType
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.*
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.api.shellRestApi
import tech.kzen.launcher.client.wrap.MaterialCard
import tech.kzen.launcher.client.wrap.MaterialCardContent
import tech.kzen.launcher.client.wrap.reactStyle


@Suppress("unused")
class ProjectList : RComponent<ProjectList.Props, RState>() {
    //-----------------------------------------------------------------------------------------------------------------
    class Props(
            var projects: Map<String, String>?,
            var didStart: (() -> Unit)?
    ) : RProps


    //-----------------------------------------------------------------------------------------------------------------
    private fun onStart(name: String, location: String) {
        console.log("onStart: name - $name | location - $location")
        async {
            shellRestApi.startProject(name, location)
            props.didStart?.invoke()
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun RBuilder.render() {
        h2 {
            +"Available Projects"
        }

        val projects = props.projects
        if (projects != null) {
            renderProjects(projects)
        }
        else {
            +"Loading..."
        }
    }


    private fun RBuilder.renderProjects(projects: Map<String, String>) {
        for (project in projects) {
            div {
                +(project.key)

                input (type = InputType.button) {
                    attrs {
                        value = "Run"
                        onClickFunction = {
                            onStart(project.key, project.value)
                        }
                    }
                }
            }
        }
    }
}
