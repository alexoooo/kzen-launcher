@file:JsModule("@mui/icons-material")
package tech.kzen.launcher.client.wrap


import react.Component
import react.ReactElement


// see: https://material-ui.com/style/icons/
// see: https://material.io/tools/icons/?style=baseline


// NB: can't create common MaterialIcon interface because 'external' doesn't support that

external interface IconProps: react.Props {
    var title: String
    var color: String
    var style: react.CSSProperties?

    var onClick: () -> Unit
}


@JsName("AddCircleOutlined")
external class AddCircleOutlinedIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("RemoveCircleOutlined")
external class RemoveCircleOutlinedIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("Stop")
external class StopIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("DeleteForever")
external class DeleteForeverIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("PlayArrow")
external class PlayArrowIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("Save")
external class SaveIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("Edit")
external class EditIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("Launch")
external class LaunchIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("Create")
external class CreateIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("Redo")
external class RedoIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("Info")
external class InfoIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("Upgrade")
external class UpgradeIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}
