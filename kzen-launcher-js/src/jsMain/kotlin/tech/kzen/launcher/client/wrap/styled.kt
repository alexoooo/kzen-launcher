package tech.kzen.launcher.client.wrap
//
//import kotlinx.css.*
//import kotlin.js.Json
//import kotlin.js.json
//
//
//fun reactStyle(handler: RuleSet): Json {
//    val style = CssBuilder().apply(handler)
//
//    val reactStyles = json()
//
//    for (e in style.declarations) {
//        reactStyles[e.key] = e.value.toString()
//    }
//
//    return reactStyles
//}