@file:JsModule("@material-ui/core")
package tech.kzen.launcher.client.wrap

import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import react.Component
import react.Props
import react.ReactElement
import kotlin.js.Json


// TODO: consolidate with materialDsl

@JsName("Button")
external class MaterialButton : Component<MaterialButtonProps, react.State> {
    override fun render(): ReactElement<MaterialButtonProps>?
}

external interface MaterialButtonProps: react.Props {
    var id: String
    var variant: String
    var color: String
    var style: Json
    var size: String
    var onClick: () -> Unit
    var disabled: Boolean
}



@JsName("IconButton")
external class MaterialIconButton : Component<MaterialIconButtonProps, react.State> {
    override fun render(): ReactElement<MaterialIconButtonProps>?
}

external interface MaterialIconButtonProps: react.Props {
//    var id: String
//    var variant: String

    var disabled: Boolean

//    var size: String
    var color: String
    var style: Json

    var onClick: () -> Unit
}




@JsName("Typography")
external class MaterialTypography : Component<TypographyProps, react.State> {
    override fun render(): ReactElement<TypographyProps>?
}


external interface TypographyProps: react.Props {
    var style: Json
}


@JsName("Card")
external class MaterialCard: Component<CardProps, react.State> {
    override fun render(): ReactElement<CardProps>?
}

external interface CardProps: react.Props {
    var style: Json

//    var classes: Json
//    var className: String

    var raised: Boolean
}



@JsName("Paper")
external class MaterialPaper: Component<PaperProps, react.State> {
    override fun render(): ReactElement<PaperProps>?
}


external interface PaperProps: Props {
    var style: Json
}


@JsName("Divider")
external class MaterialDivider: Component<Props, react.State> {
    override fun render(): ReactElement<Props>?
}



@JsName("CardContent")
external class MaterialCardContent: Component<Props, react.State> {
    override fun render(): ReactElement<Props>??
}


@JsName("CardActions")
external class MaterialCardActions: Component<Props, react.State> {
    override fun render(): ReactElement<Props>??
}


@JsName("TextField")
external class MaterialTextField: Component<MaterialTextFieldProps, react.State> {
    override fun render(): ReactElement<MaterialTextFieldProps>?
}

external interface MaterialTextFieldProps: Props {
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
external class MaterialInputLabel: Component<MaterialInputLabelProps, react.State> {
    override fun render(): ReactElement<MaterialInputLabelProps>?
}

external interface MaterialInputLabelProps: Props {
    var htmlFor: String
    var style: Json
}


@JsName("Select")
external class MaterialSelect : Component<MaterialSelectProps, react.State> {
    override fun render(): ReactElement<MaterialSelectProps>?
}

external interface MaterialSelectProps: Props {
    var value: String
    var onChange: () -> Unit

    var inputProps: Json
}


@JsName("MenuItem")
external class MaterialMenuItem : Component<MaterialMenuItemProps, react.State> {
    override fun render(): ReactElement<MaterialMenuItemProps>?
}


external interface MaterialMenuItemProps: Props {
    var value: String
}


@JsName("CircularProgress")
external class MaterialCircularProgress: Component<MaterialCircularProgressProps, react.State> {
    override fun render(): ReactElement<MaterialCircularProgressProps>?
}
external interface MaterialCircularProgressProps: Props {
    var style: Json
}




@JsName("AppBar")
external class MaterialAppBar: Component<AppBarProps, react.State> {
    override fun render(): ReactElement<AppBarProps>?
}


external interface AppBarProps: Props {
    var position: String
    var style: Json
}


@JsName("Toolbar")
external class MaterialToolbar: Component<ToolbarProps, react.State> {
    override fun render(): ReactElement<ToolbarProps>?
}

external interface ToolbarProps: Props {
    var style: Json
}


@JsName("Tabs")
external class MaterialTabs: Component<TabsProps, react.State> {
    override fun render(): ReactElement<TabsProps>?
}

external interface TabsProps: Props {
    var variant: String
    var indicatorColor: String
    var textColor: String
    var value: Int

    var onChange: (Any, Int) -> Unit

    var style: Json
}


@JsName("Tab")
external class MaterialTab: Component<TabProps, react.State> {
    override fun render(): ReactElement<TabProps>?
}

external interface TabProps: Props {
    var label: String
    var style: Json
    var icon: ReactElement<*>
}