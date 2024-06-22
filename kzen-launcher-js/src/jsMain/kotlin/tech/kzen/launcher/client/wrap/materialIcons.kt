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


@JsName("AddCircleOutline")
external class AddCircleOutlineIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("RemoveCircleOutline")
external class RemoveCircleOutlineIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("Stop")
external class StopIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("Delete")
external class DeleteIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("PlayArrow")
external class PlayArrowIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("Replay")
external class ReplayIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("KeyboardArrowUp")
external class KeyboardArrowUpIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("KeyboardArrowDown")
external class KeyboardArrowDownIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("Save")
external class SaveIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("Cancel")
external class CancelIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("Close")
external class CloseIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("ArrowDownward")
external class ArrowDownwardIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("OpenInNew")
external class OpenInNewIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("Http")
external class HttpIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("Keyboard")
external class KeyboardIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("TouchApp")
external class TouchAppIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("Message")
external class MessageIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("Texture")
external class TextureIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("Timer")
external class TimerIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("Edit")
external class EditIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("Pause")
external class PauseIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement<IconProps>?
}


@JsName("Send")
external class SendIcon: Component<IconProps, react.State> {
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



