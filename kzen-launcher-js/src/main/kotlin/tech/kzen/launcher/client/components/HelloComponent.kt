package tech.kzen.launcher.client.components


import react.*
import react.dom.div


@Suppress("unused")
class HelloComponent : RComponent<HelloComponent.Props, RState>() {
    override fun RBuilder.render() {
        div {
            +("Hello: ${props.name}")
        }
    }

    class Props(var name: String) : RProps
}
