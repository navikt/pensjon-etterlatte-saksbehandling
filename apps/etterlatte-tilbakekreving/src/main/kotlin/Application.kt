package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.config.HoconApplicationConfig
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.JacksonConverter
import io.ktor.request.header
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.IgnoreTrailingSlash
import io.ktor.routing.routing
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.tilbakekreving.config.ApplicationContext
import no.nav.etterlatte.tilbakekreving.tilbakekreving
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.security.token.support.ktor.tokenValidationSupport
import org.slf4j.event.Level
import java.util.*

fun main() {
    ApplicationContext().also {
        rapidApplication(it).start()
    }
}

fun rapidApplication(
    applicationContext: ApplicationContext,
    rapidsConnection: RapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(System.getenv().withConsumerGroupId()))
            .withKtorModule { restModule(applicationContext) }
            .build()
): RapidsConnection {
    rapidsConnection.register(object : RapidsConnection.StatusListener {
        override fun onStartup(rapidsConnection: RapidsConnection) {
            applicationContext.dataSourceBuilder.migrate()
            applicationContext.kravgrunnlagConsumer(rapidsConnection).start()
        }

        override fun onShutdown(rapidsConnection: RapidsConnection) {
            applicationContext.jmsConnectionFactory.stop()
        }
    })
    return rapidsConnection
}


fun Application.restModule(applicationContext: ApplicationContext) {
    install(Authentication) {
        applicationContext.tokenValidering(this)
    }
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }
    install(IgnoreTrailingSlash)
    install(CallLogging) {
        level = Level.INFO
        // TODO er denne aktuell for denne appen når r&r håndterer isready/isalive?
        filter { call -> !call.request.path().matches(Regex(".*/isready|.*/isalive")) }
        format { call -> "<- ${call.response.status()?.value} ${call.request.httpMethod.value} ${call.request.path()}" }
        mdc(CORRELATION_ID) { call -> call.request.header(X_CORRELATION_ID) ?: UUID.randomUUID().toString() }
    }
    install(StatusPages) {
        exception<Throwable> { cause ->
            log.error("En feil oppstod: ${cause.message}", cause)
            call.respond(HttpStatusCode.InternalServerError, "En feil oppstod: ${cause.message}")
        }
    }

    routing {
        authenticate {
            tilbakekreving(applicationContext.tilbakekrevingService)
        }
    }
}

private fun Map<String, String>.withConsumerGroupId() =
    this.toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }
