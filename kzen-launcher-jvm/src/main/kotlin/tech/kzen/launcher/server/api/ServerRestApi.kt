package tech.kzen.launcher.server.api
//
//
//import org.springframework.context.annotation.Bean
//import org.springframework.web.reactive.function.server.router
//import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
//import java.util.HashMap
//import org.springframework.web.reactive.socket.WebSocketHandler
//import org.springframework.web.reactive.HandlerMapping
//import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
//import tech.kzen.launcher.common.api.CommonRestApi
//
//
//@Suppress("unused")
////@Configuration
//class ServerRestApi(
//        private val restHandler: RestHandler
//) {
//    @Bean
//    fun counterRouter() = router {
//        GET(CommonRestApi.listArchetypes, restHandler::listArchetypes)
//
//        GET(CommonRestApi.listProjects, restHandler::listProjects)
//        GET(CommonRestApi.createProject, restHandler::createProject)
//        GET(CommonRestApi.importProject, restHandler::importProject)
//        GET(CommonRestApi.removeProject, restHandler::removeProject)
//        GET(CommonRestApi.deleteProject, restHandler::deleteProject)
//        GET(CommonRestApi.renameProject, restHandler::renameProject)
//        GET(CommonRestApi.jvmArgumentsProject, restHandler::changeArguments)
//
//        // Used for inline testing
//        GET("/shell/project", restHandler::runningProjectsDummy)
//
//        GET("/", restHandler::resource)
//        GET("/**", restHandler::resource)
//    }
//
//
//    @Bean
//    fun webSocketHandlerMapping(webSocketHandler: WebSocketHandler): HandlerMapping {
//        val map = HashMap<String, WebSocketHandler>()
//        map["/event-emitter"] = webSocketHandler
//
//        val handlerMapping = SimpleUrlHandlerMapping()
//        handlerMapping.order = -1
//        handlerMapping.urlMap = map
//        return handlerMapping
//    }
//
//
//    @Bean
//    fun handlerAdapter(): WebSocketHandlerAdapter {
//        return WebSocketHandlerAdapter()
//    }
//}