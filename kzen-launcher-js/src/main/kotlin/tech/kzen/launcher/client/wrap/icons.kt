@file:JsModule("@material-ui/icons")
package tech.kzen.launcher.client.wrap


import react.Component
import react.ReactElement
import kotlin.js.Json


// see: https://material-ui.com/style/icons/
// see: https://material.io/tools/icons/?style=baseline


// NB: can't create common MaterialIcon interface because 'external' doesn't support that

external interface IconProps: react.Props {
    var title: String
    var color: String
    var style: Json

    var onClick: () -> Unit
}


@JsName("AddCircleOutline")
external class AddCircleOutlineIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("RemoveCircleOutline")
external class RemoveCircleOutlineIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("Stop")
external class StopIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("Delete")
external class DeleteIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("PlayArrow")
external class PlayArrowIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("Replay")
external class ReplayIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("KeyboardArrowUp")
external class KeyboardArrowUpIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("KeyboardArrowDown")
external class KeyboardArrowDownIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("Save")
external class SaveIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("Cancel")
external class CancelIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("Close")
external class CloseIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("ArrowDownward")
external class ArrowDownwardIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("OpenInNew")
external class OpenInNewIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("Http")
external class HttpIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("Keyboard")
external class KeyboardIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("TouchApp")
external class TouchAppIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("Message")
external class MessageIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("Texture")
external class TextureIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("Timer")
external class TimerIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("Edit")
external class EditIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("Pause")
external class PauseIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("Send")
external class SendIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("Launch")
external class LaunchIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("Create")
external class CreateIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("Redo")
external class RedoIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}


@JsName("Info")
external class InfoIcon: Component<IconProps, react.State> {
    override fun render(): ReactElement?
}



