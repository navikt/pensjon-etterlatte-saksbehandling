package no.nav.etterlatte.tilgangsstyring

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.Hook
import io.ktor.server.application.RouteScopedPlugin
import io.ktor.server.application.call
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.request.receive
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.pipeline.PipelinePhase
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.User
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.FoedselsNummerMedGraderingDTO
import no.nav.etterlatte.libs.common.FoedselsnummerDTO
import no.nav.etterlatte.libs.common.OPPGAVEID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.sak.TilgangService
import no.nav.etterlatte.token.Saksbehandler
import no.nav.etterlatte.token.Systembruker

class PluginConfiguration {
    var harTilgangBehandling: (behandlingId: String, saksbehandlerMedRoller: SaksbehandlerMedRoller)
    -> Boolean = { _, _ -> false }
    var harTilgangTilSak: (sakId: Long, saksbehandlerMedRoller: SaksbehandlerMedRoller)
    -> Boolean = { _, _ -> false }
    var harTilgangTilOppgave: (oppgaveId: String, saksbehandlerMedRoller: SaksbehandlerMedRoller)
    -> Boolean = { _, _ -> false }
    var saksbehandlerGroupIdsByKey: Map<AzureGroup, String> = emptyMap()
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
        val bruker = call.brukerTokenInfo

        if (bruker is Systembruker) {
            return@on
        }
        if (call.request.uri.contains(TILGANG_ROUTE_PATH)) {
            return@on
        }

        if (bruker is Saksbehandler) {
            val saksbehandlerGroupIdsByKey = pluginConfig.saksbehandlerGroupIdsByKey
            val behandlingId = call.parameters[BEHANDLINGSID_CALL_PARAMETER]
            call.respond(behandlingId ?: HttpStatusCode.NotFound)
            if (!behandlingId.isNullOrEmpty()) {
                if (!pluginConfig.harTilgangBehandling(
                        behandlingId,
                        SaksbehandlerMedRoller(bruker, saksbehandlerGroupIdsByKey)
                    )
                ) {
                    call.respond(behandlingId ?: HttpStatusCode.NotFound)
                }
                return@on
            }

            val sakId = call.parameters[SAKID_CALL_PARAMETER]
            if (!sakId.isNullOrEmpty()) {
                if (!pluginConfig.harTilgangTilSak(
                        sakId.toLong(),
                        SaksbehandlerMedRoller(bruker, saksbehandlerGroupIdsByKey)
                    )
                ) {
                    call.respond(HttpStatusCode.NotFound)
                }
                return@on
            }

            val oppgaveId = call.parameters[OPPGAVEID_CALL_PARAMETER]
            if (!oppgaveId.isNullOrEmpty()) {
                if (!pluginConfig.harTilgangTilOppgave(
                        oppgaveId,
                        SaksbehandlerMedRoller(bruker, saksbehandlerGroupIdsByKey)
                    )
                ) {
                    call.respond(HttpStatusCode.NotFound)
                }
                return@on
            }
        }

        return@on
    }
}

// Disse extension funksjonene er ikke gjort i hooken ovenfor pga casting overhead
suspend inline fun PipelineContext<*, ApplicationCall>.withFoedselsnummerInternal(
    tilgangService: TilgangService,
    onSuccess: (fnr: Folkeregisteridentifikator) -> Unit
) {
    val foedselsnummerDTO = call.receive<FoedselsnummerDTO>()
    val foedselsnummer = Folkeregisteridentifikator.of(foedselsnummerDTO.foedselsnummer)
    when (brukerTokenInfo) {
        is Saksbehandler -> {
            val harTilgang = tilgangService.harTilgangTilPerson(
                foedselsnummer.value,
                Kontekst.get().appUserAsSaksbehandler().saksbehandlerMedRoller
            )
            if (harTilgang) {
                onSuccess(foedselsnummer)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        else -> onSuccess(foedselsnummer)
    }
}

suspend inline fun PipelineContext<*, ApplicationCall>.withFoedselsnummerAndGradering(
    tilgangService: TilgangService,
    onSuccess: (fnr: Folkeregisteridentifikator, gradering: AdressebeskyttelseGradering?) -> Unit
) {
    val foedselsnummerDTOmedGradering = call.receive<FoedselsNummerMedGraderingDTO>()
    val foedselsnummer = Folkeregisteridentifikator.of(foedselsnummerDTOmedGradering.foedselsnummer)
    when (brukerTokenInfo) {
        is Saksbehandler -> {
            val harTilgangTilPerson = tilgangService.harTilgangTilPerson(
                foedselsnummer.value,
                Kontekst.get().appUserAsSaksbehandler().saksbehandlerMedRoller
            )
            if (harTilgangTilPerson) {
                onSuccess(foedselsnummer, foedselsnummerDTOmedGradering.gradering)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        else -> onSuccess(foedselsnummer, foedselsnummerDTOmedGradering.gradering)
    }
}

suspend inline fun PipelineContext<*, ApplicationCall>.kunAttestant(
    onSuccess: () -> Unit
) {
    when (brukerTokenInfo) {
        is Saksbehandler -> {
            val saksbehandlerMedRoller = Kontekst.get().appUserAsSaksbehandler().saksbehandlerMedRoller
            val erAttestant = saksbehandlerMedRoller.harRolleAttestant()
            if (erAttestant) {
                onSuccess()
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Mangler attestantrolle")
            }
        }
        else -> onSuccess()
    }
}

fun <T> List<T>.filterForEnheter(
    featureToggleService: FeatureToggleService,
    toggle: FeatureToggle,
    user: User,
    filter: (item: T, enheter: List<String>) -> Boolean
) =
    if (featureToggleService.isEnabled(toggle, false)) {
        when (user) {
            is SaksbehandlerMedEnheterOgRoller -> {
                val enheter = user.enheter()
                this.filter { filter(it, enheter) }
            }
            else -> this
        }
    } else {
        this
    }