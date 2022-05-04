package no.nav.etterlatte

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.typesafe.config.ConfigFactory
import health.healthApi
import io.ktor.application.install
import io.ktor.config.tryGetString
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.request.header
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import org.slf4j.event.Level
import java.util.*

class Server(applicationContext: ApplicationContext) {
    private val engine = embeddedServer(CIO, environment = applicationEngineEnvironment {
        module {
            install(ContentNegotiation) {
                jackson {
                    registerModule(JavaTimeModule())
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                }
            }

            install(CallLogging) {
                level = Level.INFO
                filter { call -> !call.request.path().startsWith("/health") }
                format { call -> "<- ${call.response.status()?.value} ${call.request.httpMethod.value} ${call.request.path()}" }
                mdc(CORRELATION_ID) { call -> call.request.header(X_CORRELATION_ID) ?: UUID.randomUUID().toString() }
            }

//            install(Authentication) {
//                tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load()))
//            }

            routing {
                healthApi()
                brevRoute(applicationContext.brevService)
//                authenticate {
//                    route("api") {
//                       brevRoute(applicationContext.brevService)
//                    }
//                }
            }
        }
        connector {
            port = ConfigFactory.load().tryGetString("port")?.toInt() ?: 8080
        }
    })

    fun run() = engine.start(true)
}
