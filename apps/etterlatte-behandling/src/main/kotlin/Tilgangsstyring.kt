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
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.bruker
import no.nav.etterlatte.token.Saksbehandler
import no.nav.etterlatte.token.SystemBruker

class PluginConfiguration {
    var harTilgangBehandling: (behandlingId: String, saksbehandlerMedRoller: SaksbehandlerMedRoller)
    -> Boolean = { _, _ -> false }
    var harTilgangTilSak: (sakId: Long, saksbehandlerMedRoller: SaksbehandlerMedRoller)
    -> Boolean = { _, _ -> false }
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

        if (bruker is SystemBruker) {
            return@on
        }
        if (call.request.uri.contains(TILGANG_ROUTE_PATH)) {
            return@on
        }

        if (bruker is Saksbehandler) {
            val behandlingId = call.parameters[BEHANDLINGSID_CALL_PARAMETER]
            if (!behandlingId.isNullOrEmpty()) {
                if (!pluginConfig.harTilgangBehandling(behandlingId, SaksbehandlerMedRoller(bruker))) {
                    call.respond(HttpStatusCode.NotFound)
                }
                return@on
            }

            val sakId = call.parameters[SAKID_CALL_PARAMETER]
            if (!sakId.isNullOrEmpty()) {
                if (!pluginConfig.harTilgangTilSak(sakId.toLong(), SaksbehandlerMedRoller(bruker))) {
                    call.respond(HttpStatusCode.NotFound)
                }
                return@on
            }
        }

        return@on
    }
}

data class SaksbehandlerMedRoller(val saksbehandler: Saksbehandler) {
    fun harRolleStrengtFortrolig(saksbehandlerGroupIdsByKey: Map<String, String>): Boolean {
        val claims = saksbehandler.getClaims()

        if (claims != null) {
            return claims.containsClaim(
                "groups",
                saksbehandlerGroupIdsByKey["AZUREAD_STRENGT_FORTROLIG_GROUPID"]
            )
        }
        return false
    }

    fun harRolleFortrolig(saksbehandlerGroupIdsByKey: Map<String, String>): Boolean {
        val claims = saksbehandler.getClaims()

        if (claims != null) {
            return claims.containsClaim(
                "groups",
                saksbehandlerGroupIdsByKey["AZUREAD_FORTROLIG_GROUPID"]
            )
        }
        return false
    }

    fun harRolleEgenansatt(saksbehandlerGroupIdsByKey: Map<String, String>): Boolean {
        val claims = saksbehandler.getClaims()

        if (claims != null) {
            return claims.containsClaim(
                "groups",
                saksbehandlerGroupIdsByKey["AZUREAD_EGEN_ANSATT_GROUPID"]
            )
        }
        return false
    }
}