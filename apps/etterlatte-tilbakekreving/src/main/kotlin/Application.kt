package no.nav.etterlatte

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.tilbakekreving.config.ApplicationContext
import no.nav.etterlatte.tilbakekreving.tilbakekreving
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
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
): RapidsConnection =
    with(applicationContext) {
        rapidsConnection.register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                dataSourceBuilder.migrate()
                // kravgrunnlagConsumer(rapidsConnection).start() TODO trenger å sette opp kø
            }

            override fun onShutdown(rapidsConnection: RapidsConnection) {
                jmsConnectionFactory.stop()
            }
        })
        rapidsConnection
    }


fun io.ktor.server.application.Application.restModule(applicationContext: ApplicationContext) {
    install(Authentication) {
        applicationContext.tokenValidering(this)
    }
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }
    install(IgnoreTrailingSlash)
    install(CallLogging) {
        level = Level.INFO
        filter { call -> !call.request.path().matches(Regex(".*/isready|.*/isalive")) }
        format { call -> "<- ${call.response.status()?.value} ${call.request.httpMethod.value} ${call.request.path()}" }
        mdc(CORRELATION_ID) { call -> call.request.header(X_CORRELATION_ID) ?: UUID.randomUUID().toString() }
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("En feil oppstod: ${cause.message}", cause)
            call.respond(TextContent( "En feil oppstod: ${cause.message}", ContentType.Text.Plain, HttpStatusCode.InternalServerError))
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
