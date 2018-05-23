package tech.kzen.launcher.server.api

import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import org.springframework.web.reactive.socket.WebSocketHandler
import java.time.Duration.ofMillis
import reactor.core.publisher.Flux
import com.fasterxml.jackson.core.JsonProcessingException
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json
import java.time.Duration
import java.util.UUID.randomUUID




@Component
class ReactiveWebSocketHandler : WebSocketHandler {


//    private val eventFlux = Flux.generate<String> { sink ->
//        val event = randomUUID().toString()
//        try {
//            sink.next(event)
//        } catch (e: JsonProcessingException) {
//            sink.error(e)
//        }
//    }

    private val intervalFlux: Flux<String> = Flux.interval(Duration.ofMillis(1000L))
            .map { it.toString() }
//            .zipWith<String, String>(eventFlux) { time, event -> event }

    override fun handle(webSocketSession: WebSocketSession): Mono<Void> {
        return webSocketSession.send(
                intervalFlux.map { webSocketSession.textMessage(it) }
        ).and(
                webSocketSession.receive()
                        .map<String>({ it.payloadAsText })
                        .log()
        )
    }
}