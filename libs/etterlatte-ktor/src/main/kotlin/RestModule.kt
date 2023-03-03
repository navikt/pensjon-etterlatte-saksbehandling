package no.nav.etterlatte.libs.ktor

import com.fasterxml.jackson.core.JacksonException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline.ApplicationPhase.Call
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.tryGetString
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.token.System
import no.nav.security.token.support.v2.tokenValidationSupport
import org.slf4j.Logger
import org.slf4j.event.Level
import java.util.*

fun Route.adresseBeskyttelseRoute(ressursHarAdressebeskyttelse: (id: String) -> Boolean = { false }) {
    intercept(Call) {
        if (bruker is System) {
            return@intercept
        }

        val behandlingId = call.parameters[BEHANDLINGSID_CALL_PARAMETER] ?: return@intercept

        if (ressursHarAdressebeskyttelse(behandlingId)) {
            call.respond(HttpStatusCode.NotFound)
        }
        return@intercept
    }
}

fun Application.restModule(
    sikkerLogg: Logger,
    routePrefix: String? = null,
    config: ApplicationConfig = environment.config,
    routes: Route.() -> Unit
) {
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }

    install(IgnoreTrailingSlash)

    install(CallLogging) {
        level = Level.INFO
        filter { call -> !call.request.path().matches(Regex(".*/isready|.*/isalive|.*/metrics")) }
        format { call -> "<- ${call.response.status()?.value} ${call.request.httpMethod.value} ${call.request.path()}" }
        callIdMdc(CORRELATION_ID)
    }

    install(CallId) {
        retrieveFromHeader(HttpHeaders.XCorrelationId)
        generate { UUID.randomUUID().toString() }
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            if (cause.erDeserialiseringsException()) {
                sikkerLogg.error("En feil har oppstått ved deserialisering", cause)
                call.application.log.error("En feil har oppstått ved deserialisering. Se sikkerlogg for mer detaljer.")
            } else {
                call.application.log.error("En feil oppstod: ${cause.message}", cause)
            }
            call.respond(HttpStatusCode.InternalServerError, "En intern feil har oppstått")
        }
    }

    install(Authentication) {
        tokenValidationSupport(config = config)
    }

    routing {
        healthApi()
        authenticate {
            leggGrupperPaaContext(lesGrupperFraConfig(config))
            route(routePrefix ?: "") {
                routes()
            }
        }

        metrics()
    }
}

internal fun Throwable.erDeserialiseringsException(): Boolean {
    if (this is JacksonException) {
        return true
    }

    return this.cause?.erDeserialiseringsException() ?: false
}

fun lesGrupperFraConfig(config: ApplicationConfig): Grupper = ADGroup.values().associateWith { gruppe ->
    config.tryGetString(gruppe.key) ?: throw NullPointerException("Mangler id for gruppe: ${gruppe.key}")
}

typealias Grupper = Map<ADGroup, String>

object Tilgangsgrupper : ThreadLocal<Grupper>()

private fun Route.leggGrupperPaaContext(grupper: Grupper) {
    intercept(Call) {
        withContext(Dispatchers.Default + Tilgangsgrupper.asContextElement(value = grupper)) {
            proceed()
        }
        Tilgangsgrupper.remove()
    }
}