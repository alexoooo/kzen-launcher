package tech.kzen.launcher.server.api

//import org.springframework.context.annotation.Configuration
//import org.springframework.web.socket.config.annotation.StompEndpointRegistry
//import org.springframework.messaging.simp.config.MessageBrokerRegistry
//import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
//import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
//
//
//@Configuration
//@EnableWebSocketMessageBroker
//class WebSocketConfig : WebSocketMessageBrokerConfigurer {
//    override fun configureMessageBroker(config: MessageBrokerRegistry) {
//        config.enableSimpleBroker("/topic")
//        config.setApplicationDestinationPrefixes("/app")
//    }
//
//
//    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
//        registry.addEndpoint("/topic")
////                .withSockJS()
////                .setClientLibraryUrl("/bower_components/sockjs-client/dist/sockjs.min.js")
//    }
//}
