package tech.kzen.launcher.server.api


import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.router
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import java.util.HashMap
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter






@Configuration
class RestApi(
        private val restHandler: RestHandler
) {
    @Bean
    fun counterRouter() = router {
        GET("/rs/query/archetype", restHandler::archetypes)

        GET("/", restHandler::resource)
        GET("/**", restHandler::resource)
    }


    @Bean
    fun webSocketHandlerMapping(webSocketHandler: WebSocketHandler): HandlerMapping {
        val map = HashMap<String, WebSocketHandler>()
        map["/event-emitter"] = webSocketHandler

        val handlerMapping = SimpleUrlHandlerMapping()
        handlerMapping.order = -1
        handlerMapping.urlMap = map
        return handlerMapping
    }


    @Bean
    fun handlerAdapter(): WebSocketHandlerAdapter {
        return WebSocketHandlerAdapter()
    }
}