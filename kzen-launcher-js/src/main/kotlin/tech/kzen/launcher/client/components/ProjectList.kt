package tech.kzen.launcher.client.components


import react.*
import react.dom.div


@Suppress("unused")
class ProjectList : RComponent<ProjectList.Props, RState>() {
    override fun RBuilder.render() {




        for (project in props.projects) {
            div {
                +("${project.key}: ${project.value}")
            }
        }
    }

    class Props(var projects: Map<String, String>) : RProps
}
