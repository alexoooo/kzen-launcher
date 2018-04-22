package tech.kzen.launcher.server.api


import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.router


@Configuration
class RestApi(
        private val counterHandler: CounterHandler
) {
    @Bean
    fun counterRouter() = router {
        GET("/test", counterHandler::get)

        GET("/**", counterHandler::resource)




//        "/test".nest {
//            GET("", counterHandler::get)
//        }

//        "/api/counter".nest {
//            accept(MediaType.APPLICATION_JSON).nest {
//                GET("/", counterHandler::get)
//                PUT("/up", counterHandler::up)
//                PUT("/down", counterHandler::down)
//            }
//            accept(MediaType.TEXT_EVENT_STREAM).nest {
//                GET("/", counterHandler::stream)
//            }
//        }
    }
}