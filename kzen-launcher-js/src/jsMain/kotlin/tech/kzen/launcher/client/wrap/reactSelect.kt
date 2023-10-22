@file:JsModule("react-select")
package tech.kzen.launcher.client.wrap

import org.w3c.dom.HTMLElement
import react.Component
import react.ReactElement
import kotlin.js.Json


@JsName("default")
external class ReactSelect: Component<ReactSelectProps, react.State> {
    override fun render(): ReactElement<ReactSelectProps>?
}


external interface ReactSelectProps: react.Props {
    var id: String
    var value: ReactSelectOption?
    var options: Array<ReactSelectOption>
    var onChange: (ReactSelectOption) -> Unit
    var components: Json
    var menuContainerStyle: Json
    var styles: Json
    var menuPortalTarget: HTMLElement
    var placeholder: String

    var onMenuOpen: () -> Unit

    var isDisabled: Boolean

//    var id: String
//    var variant: String
//    var color: String
//    var style: ButtonStyle
//    var size: String
//    var onClick: () -> Unit
}