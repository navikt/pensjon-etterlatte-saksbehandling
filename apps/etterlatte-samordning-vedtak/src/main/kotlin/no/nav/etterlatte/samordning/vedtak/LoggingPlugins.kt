package no.nav.etterlatte.samordning.vedtak

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.Hook
import io.ktor.server.application.RouteScopedPlugin
import io.ktor.server.application.call
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.uri
import io.ktor.util.AttributeKey
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.util.pipeline.PipelinePhase
import net.logstash.logback.marker.Markers
import no.nav.etterlatte.libs.ktor.PluginConfiguration
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.token.Saksbehandler
import no.nav.etterlatte.token.Systembruker
import org.slf4j.MDC

internal val LOGGER = KtorSimpleLogger("no.nav.etterlatte.samordning.requestLogger")

private val userAttribute = AttributeKey<String>("user")
private val startTimeAttribute = AttributeKey<Long>("starttime")

private object UserIdMdcHook : Hook<suspend (ApplicationCall) -> Unit> {
    private val UserIdMdcHook: PipelinePhase = PipelinePhase("UserIdMdc")
    private val AuthenticatePhase: PipelinePhase = PipelinePhase("Authenticate")

    override fun install(
        pipeline: ApplicationCallPipeline,
        handler: suspend (ApplicationCall) -> Unit,
    ) {
        // Se Tilgangsstyring.kt
        pipeline.insertPhaseAfter(ApplicationCallPipeline.Plugins, AuthenticatePhase)
        pipeline.insertPhaseAfter(AuthenticatePhase, UserIdMdcHook)
        pipeline.intercept(UserIdMdcHook) { handler(call) }
    }
}

val userIdMdcPlugin: RouteScopedPlugin<PluginConfiguration> =
    createRouteScopedPlugin(
        name = "UserIdMdcPlugin",
        createConfiguration = ::PluginConfiguration,
    ) {
        on(UserIdMdcHook) { call ->
            val user =
                if (call.request.uri.contains("pensjon")) {
                    when (val bruker = call.brukerTokenInfo) {
                        is Systembruker -> bruker.sub
                        is Saksbehandler -> bruker.ident
                        else -> "ukjent"
                    }
                } else {
                    call.orgNummer
                }

            MDC.put("user", user)
            call.attributes.put(userAttribute, user)

            return@on
        }
    }

val serverRequestLoggerPlugin =
    createApplicationPlugin("ServerRequestLoggingPlugin") {
        onCall { call ->
            call.attributes.put(startTimeAttribute, System.currentTimeMillis())
        }

        onCallRespond { call, _ ->
            val duration = call.attributes[startTimeAttribute].let { System.currentTimeMillis() - it }
            val method = call.request.httpMethod.value
            val responseCode = call.response.status()?.value

            val markers =
                Markers.appendEntries(
                    mapOf(
                        "method" to method,
                        "response_code" to responseCode,
                        "response_time" to duration,
                        "request_uri" to sanitizedPath(call.request.path()),
                        "user" to (call.attributes.getOrNull(userAttribute) ?: "unknown"),
                    ),
                )

            LOGGER.info(markers, "Processed {} {} {} in {} ms", responseCode, method, call.request.uri, duration)
        }
    }

/**
 * For å kunne gruppere i relevant verktøy
 * - "/api/vedtak/123" -> "/api/vedtak/{id}"
 * - "/api/vedtak?noe=annet" -> "/api/vedtak"
 * @param path allerede strippet for queryparams
 */
private fun sanitizedPath(path: String): String {
    return if (path.last().isDigit()) {
        path.replaceAfterLast("/", "{id}")
    } else {
        path
    }
}
