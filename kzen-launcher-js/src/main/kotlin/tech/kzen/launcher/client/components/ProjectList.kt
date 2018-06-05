package tech.kzen.launcher.client.components


import react.*
import react.dom.div
import react.dom.fieldSet
import react.dom.legend


@Suppress("unused")
class ProjectList : RComponent<ProjectList.Props, RState>() {
    override fun RBuilder.render() {

        fieldSet {
            legend {
                +"Available projects"
            }

            for (project in props.projects) {
                div {
                    +("${project.key}: ${project.value}")
                }
            }
        }
    }

    class Props(var projects: Map<String, String>) : RProps
}
