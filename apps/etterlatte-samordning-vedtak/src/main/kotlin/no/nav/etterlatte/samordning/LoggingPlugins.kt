package no.nav.etterlatte.samordning

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.Hook
import io.ktor.server.application.RouteScopedPlugin
import io.ktor.server.application.call
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.hooks.ResponseSent
import io.ktor.server.auth.principal
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.routing.RoutingApplicationCall
import io.ktor.util.AttributeKey
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.util.pipeline.PipelinePhase
import net.logstash.logback.marker.Markers
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.MASKINPORTEN
import no.nav.etterlatte.libs.ktor.PluginConfiguration
import no.nav.etterlatte.libs.ktor.RESPONSE_TIME
import no.nav.etterlatte.libs.ktor.STARTTIME
import no.nav.etterlatte.libs.ktor.X_METHOD
import no.nav.etterlatte.libs.ktor.X_REQUEST_URI
import no.nav.etterlatte.libs.ktor.X_RESPONSE_CODE
import no.nav.etterlatte.libs.ktor.X_USER
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.samordning.vedtak.orgNummer
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import org.slf4j.MDC

internal val LOGGER = KtorSimpleLogger("no.nav.etterlatte.samordning.requestLogger")

private val userAttribute = AttributeKey<String>(X_USER)
private val startTimeAttribute = AttributeKey<Long>(STARTTIME)
private val loggingPerformed = AttributeKey<Boolean>("requestLoggingPerformed")

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
            val principal = call.principal<TokenValidationContextPrincipal>()

            val user =
                if (principal?.context?.issuers?.contains("tokenx") == true) {
                    "Selvbetjening" // Altså en borger/privatperson
                } else if (principal?.context?.issuers?.contains(AZURE_ISSUER) == true) {
                    when (val bruker = call.brukerTokenInfo) {
                        is Systembruker -> bruker.jwtTokenClaims?.getStringClaim(Claims.azp_name.name) ?: bruker.sub
                        is Saksbehandler -> bruker.jwtTokenClaims?.getStringClaim(Claims.NAVident.name) ?: ""
                        else -> "Ukjent"
                    }
                } else if (principal?.context?.issuers?.contains(MASKINPORTEN) == true) {
                    call.orgNummer
                } else {
                    "Ukjent"
                }

            MDC.put(X_USER, user)
            call.attributes.put(userAttribute, user)

            return@on
        }
    }

val serverRequestLoggerPlugin =
    createRouteScopedPlugin("ServerRequestLoggingPlugin") {
        onCall { call ->
            call.attributes.put(loggingPerformed, false)
            call.attributes.put(startTimeAttribute, System.currentTimeMillis())
        }

        on(ResponseSent) { call ->
            if (!call.attributes[loggingPerformed]) {
                val duration = call.attributes[startTimeAttribute].let { System.currentTimeMillis() - it }
                val method = call.request.httpMethod.value
                val responseCode = call.response.status()?.value
                val requestUriTemplate = extractUrlTemplate(call)

                val markers =
                    Markers.appendEntries(
                        mapOf(
                            X_METHOD to method,
                            X_RESPONSE_CODE to responseCode,
                            RESPONSE_TIME to duration,
                            X_REQUEST_URI to requestUriTemplate,
                            X_USER to (call.attributes.getOrNull(userAttribute) ?: "unknown"),
                        ),
                    )

                LOGGER.info(markers, "Processed {} {} {} in {} ms", responseCode, method, call.request.uri, duration)

                // Workaround to avoid duplicate logging due to handling of unathorized both in Authenticate-plugin and StatusPages
                call.attributes.put(loggingPerformed, true)
            }
        }
    }

/**
 * For å kunne gruppere i relevant verktøy uten unike identifikatorer, og med path params sine faktiske navn i koden.
 * - "/api/vedtak/123" -> "/api/vedtak/{vedtakId}"
 * - "/api/vedtak?fomdato=2024-01-01&noe=annet" -> "/api/vedtak?fomdato,noe"
 */
private fun extractUrlTemplate(call: ApplicationCall): String? =
    when (call) {
        is RoutingApplicationCall ->
            (call.route.parent ?: call.route) // Drop METHOD part
                .toString()
                .replace("/(authenticate \"default\")", "", true) // Alle sikrede endepunkter wrappes av authenticate
                .plus(
                    call.request.queryParameters
                        .entries()
                        .joinToString(prefix = "?", separator = ",") { it.key },
                )
        else -> null
    }
