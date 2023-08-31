package no.nav.etterlatte.libs.ktor

import com.fasterxml.jackson.core.JacksonException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.MeterBinder
import isProd
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.v2.tokenValidationSupport
import org.slf4j.Logger
import org.slf4j.event.Level
import java.util.*

fun Application.restModule(
    sikkerLogg: Logger,
    routePrefix: String? = null,
    withMetrics: Boolean = false,
    additionalMetrics: List<MeterBinder> = emptyList(),
    config: ApplicationConfig = environment.config,
    additionalValidation: ((TokenValidationContext) -> Boolean)? = null,
    routes: Route.() -> Unit
) {
    sikkerLogg.info("Sikkerlogg logger fra restModule")

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }

    install(IgnoreTrailingSlash)

    install(CallLogging) {
        level = Level.INFO
        filter { call -> !call.request.path().matches(Regex(".*/isready|.*/isalive|.*/metrics")) }
        format { call ->
            skjulAllePotensielleFnr(
                "<- ${call.response.status()?.value} ${call.request.httpMethod.value} ${call.request.path()}"
            )
        }
        callIdMdc(CORRELATION_ID)
    }

    install(CallId) {
        retrieveFromHeader(HttpHeaders.XCorrelationId)
        generate { UUID.randomUUID().toString() }
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            if (cause.erDeserialiseringsException() && isProd()) {
                sikkerLogg.error("En feil har oppstått ved deserialisering", cause)
                call.application.log.error("En feil har oppstått ved deserialisering. Se sikkerlogg for mer detaljer.")
            } else {
                call.application.log.error("En feil oppstod: ${cause.message}", cause)
            }
            call.respond(HttpStatusCode.InternalServerError, "En intern feil har oppstått")
        }
    }

    install(Authentication) {
        tokenValidationSupport(
            config = config,
            additionalValidation = additionalValidation
        )
    }

    routing {
        healthApi()
        authenticate {
            route(routePrefix ?: "") {
                routes()
            }
        }
    }

    if (withMetrics) {
        metricsModule(additionalMetrics)
    }
}

internal fun Throwable.erDeserialiseringsException(): Boolean {
    if (this is JacksonException) {
        return true
    }

    return this.cause?.erDeserialiseringsException() ?: false
}

/**
 * Bruker en regex med negativ lookbehind (?<!) og negativ lookahead (?!) for å matche alle forekomster av
 * nøyaktig 11 tall på rad ([ikke tall før, 11 tall, ikke tall etter] er tolkningen til regex'en), og bytte de
 * ut med 11 *. Ser ikke på "gyldigheten" til det som er potensielle fnr, bare fjerner alle slike forekomster.
 */
fun skjulAllePotensielleFnr(url: String): String = url.replace(Regex("(?<!\\d)\\d{11}(?!\\d)"), "***********")