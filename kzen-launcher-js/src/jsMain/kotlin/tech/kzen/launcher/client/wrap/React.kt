package tech.kzen.launcher.client.wrap


import js.objects.unsafeJso
import js.reflect.unsafeCast
import react.*
import kotlin.reflect.KClass


abstract class RComponent<P : Props, S : State> : Component<P, S> {
    constructor() : super() {
        state = unsafeJso { init() }
    }

    constructor(props: P) : super(props) {
        state = unsafeJso { init(props) }
    }

    open fun S.init() {}

    // if you use this method, don't forget to pass props to the constructor first
    open fun S.init(props: P) {}

    abstract fun ChildrenBuilder.render()

    override fun render(): ReactNode = Fragment.create { render() }
}

fun <S : State> Component<*, S>.setState(buildState: S.() -> Unit) {
    val partialState: S = unsafeJso {
        buildState()
    }
    setState(partialState)
}


// Replaces the `react.react` KClass extension that was removed in kotlin-wrappers 2026.x
// (it lived in `kotlin-react-legacy`). Bridges a class-component KClass to the modern
// ElementType so call sites can keep using `SomeComponent::class.react { ... }`.
inline val <P : Props> KClass<out Component<P, *>>.react: ComponentType<P>
    get() = unsafeCast(js)
