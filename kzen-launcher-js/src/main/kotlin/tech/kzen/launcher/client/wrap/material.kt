@file:JsModule("@material-ui/core")
package tech.kzen.launcher.client.wrap

import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import react.Component
import react.RProps
import react.RState
import react.ReactElement
import kotlin.js.Json


// TODO: consolidate with materialDsl

@JsName("Button")
external class MaterialButton : Component<MaterialButtonProps, RState> {
    override fun render(): ReactElement?
}

external interface MaterialButtonProps : RProps {
    var id: String
    var variant: String
    var color: String
    var style: Json
    var size: String
    var onClick: () -> Unit
    var disabled: Boolean
}



@JsName("IconButton")
external class MaterialIconButton : Component<MaterialIconButtonProps, RState> {
    override fun render(): ReactElement?
}

external interface MaterialIconButtonProps : RProps {
//    var id: String
//    var variant: String

    var disabled: Boolean

//    var size: String
    var color: String
    var style: Json

    var onClick: () -> Unit
}




@JsName("Typography")
external class MaterialTypography : Component<TypographyProps, RState> {
    override fun render(): ReactElement?
}


external interface TypographyProps : RProps {
    var style: Json
}


@JsName("Card")
external class MaterialCard : Component<CardProps, RState> {
    override fun render(): ReactElement?
}

external interface CardProps : RProps {
    var style: Json

//    var classes: Json
//    var className: String

    var raised: Boolean
}



@JsName("Paper")
external class MaterialPaper : Component<PaperProps, RState> {
    override fun render(): ReactElement?
}


external interface PaperProps : RProps {
    var style: Json
}


@JsName("Divider")
external class MaterialDivider : Component<RProps, RState> {
    override fun render(): ReactElement?
}



@JsName("CardContent")
external class MaterialCardContent : Component<RProps, RState> {
    override fun render(): ReactElement?
}


@JsName("CardActions")
external class MaterialCardActions : Component<RProps, RState> {
    override fun render(): ReactElement?
}


@JsName("TextField")
external class MaterialTextField : Component<MaterialTextFieldProps, RState> {
    override fun render(): ReactElement?
}

external interface MaterialTextFieldProps : RProps {
    var style: Json
    var onChange: (e: Event) -> Unit

    var onKeyDown: (e: KeyboardEvent) -> Unit
    var onKeyPress: (e: KeyboardEvent) -> Unit

    var autoFocus: Boolean

    var id: String
    var value: String
    var label: String
    var rows: Int
    var multiline: Boolean
    var fullWidth: Boolean
    var margin: String
}


//@JsName("FormControl")
//external class MaterialFormControl : Component<MaterialFormControlProps, RState> {
//    override fun render(): ReactElement?
//}
//
//external interface MaterialFormControlProps : RProps {
//}

@JsName("InputLabel")
external class MaterialInputLabel : Component<MaterialInputLabelProps, RState> {
    override fun render(): ReactElement?
}

external interface MaterialInputLabelProps : RProps {
    var htmlFor: String
    var style: Json
}


@JsName("Select")
external class MaterialSelect : Component<MaterialSelectProps, RState> {
    override fun render(): ReactElement?
}

external interface MaterialSelectProps : RProps {
    var value: String
    var onChange: () -> Unit

    var inputProps: Json
}


@JsName("MenuItem")
external class MaterialMenuItem : Component<MaterialMenuItemProps, RState> {
    override fun render(): ReactElement?
}


external interface MaterialMenuItemProps : RProps {
    var value: String
}


@JsName("CircularProgress")
external class MaterialCircularProgress : Component<MaterialCircularProgressProps, RState> {
    override fun render(): ReactElement?
}
external interface MaterialCircularProgressProps : RProps {
    var style: Json
}




@JsName("AppBar")
external class MaterialAppBar : Component<AppBarProps, RState> {
    override fun render(): ReactElement?
}


external interface AppBarProps : RProps {
    var position: String
    var style: Json
}


@JsName("Toolbar")
external class MaterialToolbar : Component<ToolbarProps, RState> {
    override fun render(): ReactElement?
}

external interface ToolbarProps : RProps {
    var style: Json
}


@JsName("Tabs")
external class MaterialTabs: Component<TabsProps, RState> {
    override fun render(): ReactElement?
}

external interface TabsProps: RProps {
    var variant: String
    var indicatorColor: String
    var textColor: String
    var value: Int

    var onChange: (Any, Int) -> Unit

    var style: Json
}


@JsName("Tab")
external class MaterialTab: Component<TabProps, RState> {
    override fun render(): ReactElement?
}

external interface TabProps: RProps {
    var label: String
    var style: Json
    var icon: ReactElement
}