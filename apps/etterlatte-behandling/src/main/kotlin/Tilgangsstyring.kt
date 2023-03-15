package no.nav.etterlatte

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.Hook
import io.ktor.server.application.RouteScopedPlugin
import io.ktor.server.application.call
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelinePhase
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.FNR_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.bruker
import no.nav.etterlatte.token.System

class PluginConfiguration {
    var behandlingIdHarAdressebeskyttelse: (behandlingId: String) -> Boolean = { false }
    var fnrHarAdressebeskyttelse: (fnr: String) -> Boolean = { false }
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
        pipeline.insertPhaseBefore(ApplicationCallPipeline.Call, AdressebeskyttelseHook)
        pipeline.intercept(AdressebeskyttelseHook) { handler(call) }
    }
}

const val TILGANG_ROUTE_PATH = "tilgang"

val adressebeskyttelsePlugin: RouteScopedPlugin<PluginConfiguration> = createRouteScopedPlugin(
    name = "Adressebeskyttelsesplugin",
    createConfiguration = ::PluginConfiguration
) {
    on(AdressebeskyttelseHook) { call ->
        val bruker = call.bruker
        if (bruker is System) {
            return@on
        }
        if (call.request.uri.contains(TILGANG_ROUTE_PATH)) {
            return@on
        }
        val behandlingId = call.parameters[BEHANDLINGSID_CALL_PARAMETER]

        if (!behandlingId.isNullOrEmpty()) {
            if (pluginConfig.behandlingIdHarAdressebeskyttelse(behandlingId)) {
                call.respond(HttpStatusCode.NotFound)
            }
            return@on
        }

        val fnr = call.parameters[FNR_CALL_PARAMETER]
        if (!fnr.isNullOrEmpty()) {
            if (pluginConfig.fnrHarAdressebeskyttelse(fnr)) {
                call.respond(HttpStatusCode.NotFound)
            }
            return@on
        }
        return@on
    }
}