package tech.kzen.launcher.server.api


import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.router
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import java.util.HashMap
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import tech.kzen.launcher.common.CommonApi


@Configuration
class ServerRestApi(
        private val restHandler: RestHandler
) {
    @Bean
    fun counterRouter() = router {
        GET(CommonApi.listArchetypes, restHandler::listArchetypes)

        GET(CommonApi.listProjects, restHandler::listProjects)
        GET(CommonApi.createProject, restHandler::createProject)
        GET(CommonApi.importProject, restHandler::importProject)
        GET(CommonApi.removeProject, restHandler::removeProject)
        GET(CommonApi.deleteProject, restHandler::deleteProject)
        GET(CommonApi.renameProject, restHandler::renameProject)

        // Used for inline testing
        GET("/shell/project", restHandler::runningProjectsDummy)

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