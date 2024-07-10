package no.nav.etterlatte.libs.ktor

import com.fasterxml.jackson.core.JacksonException
import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.github.smiley4.ktorswaggerui.routing.openApiSpec
import io.github.smiley4.ktorswaggerui.routing.swaggerUI
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.callloging.processingTimeMillis
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.MeterBinder
import no.nav.etterlatte.libs.common.isProd
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktor.feilhaandtering.StatusPagesKonfigurasjon
import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.v2.tokenValidationSupport
import org.slf4j.Logger
import org.slf4j.event.Level
import java.util.UUID

fun Application.restModule(
    sikkerLogg: Logger,
    routePrefix: String? = null,
    withMetrics: Boolean = false,
    additionalMetrics: List<MeterBinder> = emptyList(),
    config: ApplicationConfig = environment.config,
    additionalValidation: ((TokenValidationContext) -> Boolean)? = null,
    routes: (Route.() -> Unit)? = null,
    authenticatedRoutes: Route.() -> Unit,
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
            val responseTime = call.processingTimeMillis()
            val status = call.response.status()?.value
            val method = call.request.httpMethod.value
            val path = call.request.path()

            skjulAllePotensielleFnr(
                "<- $status $method $path in $responseTime ms",
            )
        }
        callIdMdc(CORRELATION_ID)

        mdc(X_METHOD) { call -> call.request.httpMethod.value }
        mdc(X_INBOUND_URI) { call -> call.request.path() }

        mdc(X_USER) { call ->
            call.request.header("Authorization")?.let {
                val token = JwtToken(it.substringAfterLast("Bearer "))
                val jwtTokenClaims = token.jwtTokenClaims
                jwtTokenClaims.get(Claims.NAVident.name) as? String // human
                    ?: token.jwtTokenClaims.get(Claims.azp_name.name) as? String // system/app-user
            }
        }
    }

    install(CallId) {
        retrieveFromHeader(HttpHeaders.XCorrelationId)
        generate { UUID.randomUUID().toString() }
    }

    install(Authentication) {
        tokenValidationSupport(
            config = config,
            additionalValidation = additionalValidation,
        )
    }

    install(StatusPages, StatusPagesKonfigurasjon(sikkerLogg).config)
    install(SwaggerUI) {
        info {
            title = "Etterlatte"
            contact {
                url = "https://github.com/navikt/pensjon-etterlatte-saksbehandling"
            }
        }
    }

    routing {
        healthApi()
        authenticate {
            route(routePrefix ?: "") {
                authenticatedRoutes()
            }
        }
        if (!isProd()) {
            route("swagger") {
                swaggerUI("/api.json")
            }
            route("api.json") {
                openApiSpec()
            }
        }
        if (routes != null) {
            routes()
        }
    }

    if (withMetrics) {
        metricsRoute(additionalMetrics)
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
