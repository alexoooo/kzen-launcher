package tech.kzen.launcher.server

//import org.springframework.boot.runApplication
//import org.springframework.web.reactive.socket.WebSocketMessage
//import reactor.core.publisher.Mono
//import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
//import org.springframework.web.reactive.socket.client.WebSocketClient
//import java.net.URI
//import java.time.Duration
//
//
//fun main(args: Array<String>) {
//    val client = ReactorNettyWebSocketClient()
//    client.execute(
//            URI.create("ws://localhost:8080/event-emitter")
//    ) { session ->
//        session.send(
//                Mono.just(session.textMessage("event-spring-reactive-client-websocket")))
//                .thenMany(session.receive()
//                        .map<String>({ it.payloadAsText })
//                        .log())
//                .then()
//    }.block(Duration.ofSeconds(10L))
//}
