package no.nav.etterlatte.libs.ktor

import com.fasterxml.jackson.core.JacksonException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationCallPipeline.ApplicationPhase.Call
import io.ktor.server.application.Hook
import io.ktor.server.application.call
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.principal
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
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import no.nav.security.token.support.v2.tokenValidationSupport
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.util.*

class PluginConfiguration {
    var canAccessAdressebeskyttelse: (id: String) -> Boolean = { false }
}

private object AdressebeskyttelseHook : Hook<suspend (ApplicationCall) -> Unit> {
    override fun install(
        pipeline: ApplicationCallPipeline,
        handler: suspend (ApplicationCall) -> Unit
    ) {
        pipeline.intercept(Call) { handler(call) }
    }
}

fun Route.sjekkKode6(canAccessAdressebeskyttelse: (id: String) -> Boolean = { false }) {
    logger.info("Route interceptor")
    intercept(Call) {
        val claims = call.principal<TokenValidationContextPrincipal>()
            ?.context
            ?.getJwtToken("azure")
            ?.jwtTokenClaims
        val firstvalidtokenclaims =
            call.principal<TokenValidationContextPrincipal>()?.context?.firstValidToken?.get()?.jwtTokenClaims
        logger.info(
            "har info om saksbehandeler, navident: " +
                "${call.principal<TokenValidationContextPrincipal>()
                    ?.context?.firstValidToken?.get()?.jwtTokenClaims?.get(
                        "NAVident"
                    )}"
        )

        logger.info("${firstvalidtokenclaims?.get("oid")} ${firstvalidtokenclaims?.get("sub")}")
        val oid = claims?.get("oid").toString()
        val sub = claims?.get("sub").toString()
        // TODO: hvis begge er null?
        val isMaskinToMaskinRequest: Boolean = oid == sub
        logger.info(
            "$oid $sub Er maskin til maskin request $isMaskinToMaskinRequest navident ${claims?.get("NAVident")}"
        )
        if (isMaskinToMaskinRequest) {
            logger.info("returns on isMaskinToMaskinRequest")
            return@intercept
        }
        val behandlingId = call.parameters["behandlingsid"] ?: return@intercept

        logger.info("params behandlingId $behandlingId")

        val canAccess = canAccessAdressebeskyttelse(behandlingId)
        logger.info("kan se adressebeskyttelse $canAccess")
        if (canAccessAdressebeskyttelse(behandlingId)) {
            logger.info("Kan aksesse adressebeskyttelse for behandlingId $behandlingId")
            return@intercept
        }
        logger.info("Not found ")
        call.respond(HttpStatusCode.NotFound)
    }
}

val logger: Logger = LoggerFactory.getLogger("Adressebeskyttelselogger")

// TODO: kan evt prøve med en Route.func extension function
val adressebeskyttelsePlugin = createApplicationPlugin(
    name = "Adressebeskyttelsesplugin",
    createConfiguration = ::PluginConfiguration
) {
    // AuthenticationChecked
    on(AuthenticationChecked) { call ->
        logger.info("Sjekker adressebeskyttelse interceptor")

        val claims = call.authentication.principal<TokenValidationContextPrincipal>()
            ?.context
            ?.getJwtToken("azure")
            ?.jwtTokenClaims

        val oid = claims?.get("oid").toString()
        val sub = claims?.get("sub").toString()
        // TODO: hvis begge er null?
        val isMaskinToMaskinRequest: Boolean = oid == sub
        logger.info(
            "$oid $sub Er maskin til maskin request $isMaskinToMaskinRequest navident ${claims?.get("NAVident")}"
        )
        if (isMaskinToMaskinRequest) {
            logger.info("returns on isMaskinToMaskinRequest")
            return@on
        }
        val behandlingId = call.parameters["behandlingsid"] ?: return@on

        logger.info("params behandlingId $behandlingId")

        val canAccessAdressebeskyttelse = pluginConfig.canAccessAdressebeskyttelse(behandlingId)
        logger.info("kan se adressebeskyttelse $canAccessAdressebeskyttelse")
        if (pluginConfig.canAccessAdressebeskyttelse(behandlingId)) {
            logger.info("Kan aksesse adressebeskyttelse for behandlingId $behandlingId")
            return@on
        }
        logger.info("Not found ")
        call.respond(HttpStatusCode.NotFound)
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