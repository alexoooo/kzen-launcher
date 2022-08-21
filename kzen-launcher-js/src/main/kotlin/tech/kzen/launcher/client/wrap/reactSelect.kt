@file:JsModule("react-select")
package tech.kzen.launcher.client.wrap

import react.Component
import react.ReactElement
import kotlin.js.Json


//@JsName("Select")
//external class ReactSelect : Component<ReactSelectProps, RState> {
//    override fun render(): ReactElement?
//}
//@JsModule("react-select")
//@JsName("Select")
//@JsName("SelectBase")


// see: https://codesandbox.io/s/ly87zo23kl
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

//    var id: String
//    var variant: String
//    var color: String
//    var style: ButtonStyle
//    var size: String
//    var onClick: () -> Unit
}


