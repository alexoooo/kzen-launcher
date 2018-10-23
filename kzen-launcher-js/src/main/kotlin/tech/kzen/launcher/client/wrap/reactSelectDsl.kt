package tech.kzen.launcher.client.wrap

import react.*


class ReactSelectOption(
        val value: String,
        val label: String)


fun RBuilder.materialReactSelectController(props: RProps): ReactElement =
        child(MaterialTextField::class) {
//            attrs.name = name
        }
//
//class MaterialReactSelect : Component<ReactSelectProps, RState> {
//    override fun render(): ReactElement?
//}