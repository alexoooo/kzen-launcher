@file:JsModule("react-select")
package tech.kzen.launcher.client.wrap

import react.Component
import react.RProps
import react.RState
import react.ReactElement
import kotlin.js.Json


//@JsName("Select")
//external class ReactSelect : Component<ReactSelectProps, RState> {
//    override fun render(): ReactElement?
//}
//@JsModule("react-select")
//@JsName("Select")
//@JsName("SelectBase")
@JsName("default")
external class ReactSelect : Component<ReactSelectProps, RState> {
    override fun render(): ReactElement?
}


external interface ReactSelectProps : RProps {
    var value: ReactSelectOption?

    var options: Array<ReactSelectOption>

    var onChange: (ReactSelectOption) -> Unit

//    var id: String
//    var variant: String
//    var color: String
//    var style: ButtonStyle
//    var size: String
//    var onClick: () -> Unit
}


