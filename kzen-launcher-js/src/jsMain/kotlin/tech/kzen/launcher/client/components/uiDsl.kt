package tech.kzen.launcher.client.components

import js.objects.unsafeJso
import mui.material.Card
import mui.material.CardContent
import mui.material.CircularProgress
import mui.material.TextField
import mui.material.Typography
import mui.material.styles.TypographyVariant
import mui.system.sx
import react.ChildrenBuilder
import react.Component
import react.ReactNode
import react.dom.onChange
import tech.kzen.launcher.client.wrap.IconProps
import tech.kzen.launcher.client.wrap.react
import web.cssom.BoxShadow
import web.cssom.Color
import web.cssom.Margin
import web.cssom.NamedColor
import web.cssom.em
import web.cssom.px
import web.html.HTMLInputElement
import kotlin.reflect.KClass


// Shared view fragments for the launcher screens — extracted from the copy-pasted blocks that recurred
// across NewProjectScreen / ManageProjectsScreen / ProjectList / ProjectItem / ProjectRunning.


// The white surface panel every screen section sits in: a rounded, softly-elevated Card with the standard
// white fill + 2em margin, wrapping its content in CardContent.
fun ChildrenBuilder.whiteCard(block: ChildrenBuilder.() -> Unit) {
    Card {
        sx {
            backgroundColor = NamedColor.white
            margin = Margin(2.em, 2.em, 2.em, 2.em)
            borderRadius = 12.px
            boxShadow = BoxShadow(0.px, 2.px, 8.px, Color("rgba(0, 0, 0, 0.10)"))
        }

        CardContent {
            block()
        }
    }
}


// A section heading (MUI h6 Typography, muted, hugging the top of its CardContent).
fun ChildrenBuilder.sectionHeading(text: String) {
    Typography {
        variant = TypographyVariant.h6
        sx {
            marginBottom = 0.5.em
            color = Color("#455a64")
        }
        +text
    }
}


// The 46em labelled text field used by every name / path / rename / jvm-args input —
//  matches the New Project Type dropdown so form rows line up.
fun ChildrenBuilder.wideTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    TextField {
        sx {
            width = 46.em
        }

        this.label = ReactNode(label)
        this.value = value

        onChange = {
            val target = it.target as HTMLInputElement
            onValueChange(target.value)
        }
    }
}


// A Material icon rendered with the small right margin used for button leading-icons.
fun ChildrenBuilder.buttonIcon(icon: KClass<out Component<IconProps, *>>) {
    icon.react {
        style = unsafeJso {
            marginRight = 0.25.em
        }
    }
}


// Stands in for a button's leading icon while that button's action is in flight — same size and
//  right margin as buttonIcon, so the label doesn't shift when the spinner appears.
fun ChildrenBuilder.buttonSpinner() {
    CircularProgress {
        sx {
            width = 1.em
            height = 1.em
            marginRight = 0.25.em
        }
    }
}
