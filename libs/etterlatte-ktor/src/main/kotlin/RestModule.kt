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
import io.ktor.server.application.RouteScopedPlugin
import io.ktor.server.application.call
import io.ktor.server.application.createRouteScopedPlugin
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
import io.ktor.util.pipeline.PipelinePhase
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.token.System
import no.nav.security.token.support.v2.tokenValidationSupport
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.util.*

class PluginConfiguration {
    var ressursHarAdressebeskyttelse: (id: String) -> Boolean = { false }
}

private object AdressebeskyttelseHook : Hook<suspend (ApplicationCall) -> Unit> {
    private val AdressebeskyttelseHook: PipelinePhase = PipelinePhase("Adressebeskyttelse")
    private val AuthenticatePhase: PipelinePhase = PipelinePhase("Authenticate")
    override fun install(
        pipeline: ApplicationCallPipeline,
        handler: suspend (ApplicationCall) -> Unit
    ) {
        // Inspirasjon AuthenticationChecked
        pipeline.insertPhaseAfter(ApplicationCallPipeline.Plugins, AuthenticatePhase)
        pipeline.insertPhaseAfter(AuthenticatePhase, AdressebeskyttelseHook)
        pipeline.insertPhaseBefore(Call, AdressebeskyttelseHook)
        pipeline.intercept(AdressebeskyttelseHook) { handler(call) }
    }
}

val logger: Logger = LoggerFactory.getLogger("Adressebeskyttelselogger")

val adressebeskyttelsePlugin: RouteScopedPlugin<PluginConfiguration> = createRouteScopedPlugin(
    name = "Adressebeskyttelsesplugin",
    createConfiguration = ::PluginConfiguration
) {
    on(AdressebeskyttelseHook) { call ->
        val log = call.application.log
        val bruker = call.bruker
        if (bruker is System) {
            return@on
        }

        val behandlingId = call.parameters[BEHANDLINGSID_CALL_PARAMETER] ?: return@on

        if (pluginConfig.ressursHarAdressebeskyttelse(behandlingId)) {
            log.info("Logs not found in hook for $behandlingId")
            call.respond(HttpStatusCode.NotFound)
        }
        return@on
    }
}

fun Application.restModule(
    sikkerLogg: Logger,
    routePrefix: String? = null,
    config: ApplicationConfig = environment.config,
    harAdressebeskyttelseFunc: ((id: String) -> Boolean)? = null,
    routesUtenHookInterceptor: (Route.() -> Unit)? = null,
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
                if (harAdressebeskyttelseFunc != null) {
                    install(adressebeskyttelsePlugin) {
                        ressursHarAdressebeskyttelse = { behandlingId ->
                            harAdressebeskyttelseFunc(behandlingId)
                        }
                    }
                }
            }
            if (routesUtenHookInterceptor != null) {
                route("tilgang") {
                    routesUtenHookInterceptor()
                }
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