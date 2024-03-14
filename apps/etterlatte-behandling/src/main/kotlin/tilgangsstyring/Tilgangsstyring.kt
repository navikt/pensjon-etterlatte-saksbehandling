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
import no.nav.etterlatte.SystemUser
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.FoedselsNummerMedGraderingDTO
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.ktor.route.KLAGEID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.OPPGAVEID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.klageId
import no.nav.etterlatte.libs.ktor.route.oppgaveId
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.sak.TilgangService

class PluginConfiguration {
    var harTilgangBehandling: (behandlingId: String, saksbehandlerMedRoller: SaksbehandlerMedRoller)
    -> Boolean = { _, _ -> false }
    var harTilgangTilSak: (sakId: Long, saksbehandlerMedRoller: SaksbehandlerMedRoller)
    -> Boolean = { _, _ -> false }
    var harTilgangTilOppgave: (oppgaveId: String, saksbehandlerMedRoller: SaksbehandlerMedRoller)
    -> Boolean = { _, _ -> false }
    var harTilgangTilKlage: (klageId: String, saksbehandlerMedRoller: SaksbehandlerMedRoller)
    -> Boolean = { _, _ -> false }
    var saksbehandlerGroupIdsByKey: Map<AzureGroup, String> = emptyMap()
}

private object AdressebeskyttelseHook : Hook<suspend (ApplicationCall) -> Unit> {
    private val AdressebeskyttelseHook: PipelinePhase = PipelinePhase("Adressebeskyttelse")
    private val AuthenticatePhase: PipelinePhase = PipelinePhase("Authenticate")

    override fun install(
        pipeline: ApplicationCallPipeline,
        handler: suspend (ApplicationCall) -> Unit,
    ) {
        // Inspirasjon AuthenticationChecked
        pipeline.insertPhaseAfter(ApplicationCallPipeline.Plugins, AuthenticatePhase)
        pipeline.insertPhaseAfter(AuthenticatePhase, AdressebeskyttelseHook)
        pipeline.insertPhaseBefore(ApplicationCallPipeline.Call, AdressebeskyttelseHook)
        pipeline.intercept(AdressebeskyttelseHook) { handler(call) }
    }
}

const val TILGANG_ROUTE_PATH = "tilgang"

val adressebeskyttelsePlugin: RouteScopedPlugin<PluginConfiguration> =
    createRouteScopedPlugin(
        name = "Adressebeskyttelsesplugin",
        createConfiguration = ::PluginConfiguration,
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
                val behandlingId = call.parameters[BEHANDLINGID_CALL_PARAMETER]
                if (!behandlingId.isNullOrEmpty()) {
                    if (!pluginConfig.harTilgangBehandling(
                            behandlingId,
                            SaksbehandlerMedRoller(bruker, saksbehandlerGroupIdsByKey),
                        )
                    ) {
                        call.respond(HttpStatusCode.NotFound)
                    }
                    return@on
                }

                val sakId = call.parameters[SAKID_CALL_PARAMETER]
                if (!sakId.isNullOrEmpty()) {
                    if (!pluginConfig.harTilgangTilSak(
                            sakId.toLong(),
                            SaksbehandlerMedRoller(bruker, saksbehandlerGroupIdsByKey),
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
                            SaksbehandlerMedRoller(bruker, saksbehandlerGroupIdsByKey),
                        )
                    ) {
                        call.respond(HttpStatusCode.NotFound)
                    }
                    return@on
                }

                val klageId = call.parameters[KLAGEID_CALL_PARAMETER]
                if (!klageId.isNullOrEmpty()) {
                    if (!pluginConfig.harTilgangTilKlage(
                            klageId,
                            SaksbehandlerMedRoller(bruker, saksbehandlerGroupIdsByKey),
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
    onSuccess: (fnr: Folkeregisteridentifikator) -> Unit,
) {
    val foedselsnummerDTO = call.receive<FoedselsnummerDTO>()
    val foedselsnummer = Folkeregisteridentifikator.of(foedselsnummerDTO.foedselsnummer)
    when (brukerTokenInfo) {
        is Saksbehandler -> {
            val harTilgang =
                inTransaction {
                    tilgangService.harTilgangTilPerson(
                        foedselsnummer.value,
                        Kontekst.get().appUserAsSaksbehandler().saksbehandlerMedRoller,
                    )
                }
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
    onSuccess: (fnr: Folkeregisteridentifikator, gradering: AdressebeskyttelseGradering?) -> Unit,
) {
    val foedselsnummerDTOmedGradering = call.receive<FoedselsNummerMedGraderingDTO>()
    val foedselsnummer = Folkeregisteridentifikator.of(foedselsnummerDTOmedGradering.foedselsnummer)
    when (brukerTokenInfo) {
        is Saksbehandler -> {
            val harTilgangTilPerson =
                inTransaction {
                    tilgangService.harTilgangTilPerson(
                        foedselsnummer.value,
                        Kontekst.get().appUserAsSaksbehandler().saksbehandlerMedRoller,
                    )
                }
            if (harTilgangTilPerson) {
                onSuccess(foedselsnummer, foedselsnummerDTOmedGradering.gradering)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        else -> onSuccess(foedselsnummer, foedselsnummerDTOmedGradering.gradering)
    }
}

fun <T> hentNullableVerdi(block: () -> T) =
    try {
        block()
    } catch (_: NullPointerException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

fun PipelineContext<*, ApplicationCall>.sjekkSkrivetilgang(
    sakId: Long? = null,
    enhetNr: String? = null,
): Boolean {
    return when (val user = Kontekst.get().AppUser) {
        is SaksbehandlerMedEnheterOgRoller -> {
            val enhetNrSomSkalTestes =
                when (enhetNr) {
                    null -> {
                        Kontekst.get().sakTilgangDao.let { sakTilgangDao ->
                            val sakEnhetNr =
                                sakId?.let {
                                    sakTilgangDao.hentSakMedGraderingOgSkjerming(it)?.enhetNr
                                } ?: hentNullableVerdi { this.sakId }?.let {
                                    sakTilgangDao.hentSakMedGraderingOgSkjerming(it)?.enhetNr
                                }
                            val behandlingEnhetNr =
                                hentNullableVerdi { behandlingId }?.let {
                                    sakTilgangDao.hentSakMedGraderingOgSkjermingPaaBehandling(it.toString())?.enhetNr
                                }
                            val oppgaveEnhetNr =
                                hentNullableVerdi { oppgaveId }?.let {
                                    sakTilgangDao.hentSakMedGraderingOgSkjermingPaaOppgave(it.toString())?.enhetNr
                                }
                            val klageEnhetNr =
                                hentNullableVerdi { klageId }?.let {
                                    sakTilgangDao.hentSakMedGraderingOgSkjermingPaaKlage(it.toString())?.enhetNr
                                }

                            listOfNotNull(sakEnhetNr, behandlingEnhetNr, oppgaveEnhetNr, klageEnhetNr).firstOrNull()
                        }
                    }

                    else -> enhetNr
                }

            when (enhetNrSomSkalTestes) {
                null -> false
                else -> user.enheterMedSkrivetilgang().contains(enhetNrSomSkalTestes)
            }
        }
        is SystemUser -> true
        else -> false
    }
}

suspend inline fun PipelineContext<*, ApplicationCall>.kunSkrivetilgang(
    sakId: Long? = null,
    enhetNr: String? = null,
    onSuccess: () -> Unit,
) {
    when (sjekkSkrivetilgang(sakId, enhetNr)) {
        true -> onSuccess()
        false -> call.respond(HttpStatusCode.Forbidden)
    }
}

suspend inline fun PipelineContext<*, ApplicationCall>.kunSaksbehandlerMedSkrivetilgang(
    sakId: Long? = null,
    enhetNr: String? = null,
    onSuccess: (Saksbehandler) -> Unit,
) {
    when (val token = brukerTokenInfo) {
        is Saksbehandler -> {
            when (sjekkSkrivetilgang(sakId, enhetNr)) {
                true -> onSuccess(token)
                false -> call.respond(HttpStatusCode.Forbidden)
            }
        }

        else -> call.respond(HttpStatusCode.Forbidden)
    }
}

suspend inline fun PipelineContext<*, ApplicationCall>.kunAttestant(onSuccess: () -> Unit) {
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
