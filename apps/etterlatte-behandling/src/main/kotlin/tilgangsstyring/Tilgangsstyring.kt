package no.nav.etterlatte.tilgangsstyring

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.Hook
import io.ktor.server.application.RouteScopedPlugin
import io.ktor.server.application.application
import io.ktor.server.application.call
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.log
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.pipeline.PipelinePhase
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.SystemUser
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.ktor.route.CallParamAuthId
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.sak.TilgangService

class PluginConfiguration {
    var harTilgangBehandling: (behandlingId: String, saksbehandlerMedRoller: SaksbehandlerMedRoller)
    -> Boolean = { _, _ -> false }
    var harTilgangTilSak: (sakId: no.nav.etterlatte.libs.common.sak.SakId, saksbehandlerMedRoller: SaksbehandlerMedRoller)
    -> Boolean = { _, _ -> false }
    var harTilgangTilOppgave: (oppgaveId: String, saksbehandlerMedRoller: SaksbehandlerMedRoller)
    -> Boolean = { _, _ -> false }
    var harTilgangTilKlage: (klageId: String, saksbehandlerMedRoller: SaksbehandlerMedRoller)
    -> Boolean = { _, _ -> false }
    var harTilgangTilGenerellBehandling: (generellbehandlingId: String, saksbehandlerMedRoller: SaksbehandlerMedRoller)
    -> Boolean = { _, _ -> false }
    var harTilgangTilTilbakekreving: (klageId: String, saksbehandlerMedRoller: SaksbehandlerMedRoller)
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
                val funnetCallIdParametersType = CallParamAuthId.entries.firstOrNull { call.parameters.contains(it.value) }
                if (funnetCallIdParametersType == null) {
                    return@on
                } else {
                    val idForRequest = call.parameters[funnetCallIdParametersType.value]!!
                    when (funnetCallIdParametersType) {
                        CallParamAuthId.BEHANDLINGID -> {
                            if (!pluginConfig.harTilgangBehandling(
                                    idForRequest,
                                    SaksbehandlerMedRoller(bruker, saksbehandlerGroupIdsByKey),
                                )
                            ) {
                                call.respond(HttpStatusCode.NotFound)
                            }
                            return@on
                        }
                        CallParamAuthId.SAKID -> {
                            if (!pluginConfig.harTilgangTilSak(
                                    idForRequest.toLong(),
                                    SaksbehandlerMedRoller(bruker, saksbehandlerGroupIdsByKey),
                                )
                            ) {
                                call.respond(HttpStatusCode.NotFound)
                            }
                            return@on
                        }
                        CallParamAuthId.OPPGAVEID -> {
                            if (!pluginConfig.harTilgangTilOppgave(
                                    idForRequest,
                                    SaksbehandlerMedRoller(bruker, saksbehandlerGroupIdsByKey),
                                )
                            ) {
                                call.respond(HttpStatusCode.NotFound)
                            }
                            return@on
                        }
                        CallParamAuthId.KLAGEID -> {
                            if (!pluginConfig.harTilgangTilKlage(
                                    idForRequest,
                                    SaksbehandlerMedRoller(bruker, saksbehandlerGroupIdsByKey),
                                )
                            ) {
                                call.respond(HttpStatusCode.NotFound)
                            }
                            return@on
                        }
                        CallParamAuthId.GENERELLBEHANDLINGID -> {
                            if (!pluginConfig.harTilgangTilGenerellBehandling(
                                    idForRequest,
                                    SaksbehandlerMedRoller(bruker, saksbehandlerGroupIdsByKey),
                                )
                            ) {
                                call.respond(HttpStatusCode.NotFound)
                            }
                            return@on
                        }
                        CallParamAuthId.TILBAKEKREVINGID -> {
                            if (!pluginConfig.harTilgangTilTilbakekreving(
                                    idForRequest,
                                    SaksbehandlerMedRoller(bruker, saksbehandlerGroupIdsByKey),
                                )
                            ) {
                                call.respond(HttpStatusCode.NotFound)
                            }
                            return@on
                        }
                    }
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
                tilgangService.harTilgangTilPerson(
                    foedselsnummer.value,
                    Kontekst.get().appUserAsSaksbehandler().saksbehandlerMedRoller,
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

fun PipelineContext<*, ApplicationCall>.sjekkSkrivetilgang(
    sakId: no.nav.etterlatte.libs.common.sak.SakId? = null,
    enhetNr: String? = null,
): Boolean {
    application.log.debug("Sjekker skrivetilgang")
    return when (val user = Kontekst.get().AppUser) {
        is SaksbehandlerMedEnheterOgRoller -> {
            val enhetNrSomSkalTestes =
                when (enhetNr) {
                    null -> finnSkriveTilgangForId(sakId)
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

private fun PipelineContext<*, ApplicationCall>.finnSkriveTilgangForId(sakId: no.nav.etterlatte.libs.common.sak.SakId? = null): String? {
    val sakTilgangDao = Kontekst.get().sakTilgangDao
    if (sakId != null) {
        return sakTilgangDao.hentSakMedGraderingOgSkjerming(sakId)?.enhetNr
    }
    val funnetCallIdParametersType = CallParamAuthId.entries.firstOrNull { call.parameters.contains(it.value) }
    return if (funnetCallIdParametersType == null) {
        application.log.warn("Fant ingen pathparam i url: ${call.request.path()} params: ${call.parameters}")
        null
    } else {
        val idForRequest = call.parameters[funnetCallIdParametersType.value]!!
        when (funnetCallIdParametersType) {
            CallParamAuthId.BEHANDLINGID -> sakTilgangDao.hentSakMedGraderingOgSkjermingPaaBehandling(idForRequest)?.enhetNr
            CallParamAuthId.SAKID -> sakTilgangDao.hentSakMedGraderingOgSkjerming(idForRequest.toLong())?.enhetNr
            CallParamAuthId.OPPGAVEID -> sakTilgangDao.hentSakMedGraderingOgSkjermingPaaOppgave(idForRequest)?.enhetNr
            CallParamAuthId.KLAGEID -> sakTilgangDao.hentSakMedGraderingOgSkjermingPaaKlage(idForRequest)?.enhetNr
            CallParamAuthId.GENERELLBEHANDLINGID ->
                sakTilgangDao.hentSakMedGraderingOgSkjermingPaaGenerellbehandling(idForRequest)?.enhetNr
            CallParamAuthId.TILBAKEKREVINGID ->
                sakTilgangDao.hentSakMedGraderingOgSkjermingPaaTilbakekreving(idForRequest)?.enhetNr
        }
    }
}

suspend inline fun PipelineContext<*, ApplicationCall>.kunSkrivetilgang(
    sakId: no.nav.etterlatte.libs.common.sak.SakId? = null,
    enhetNr: String? = null,
    onSuccess: () -> Unit,
) {
    application.log.debug("Sjekker skrivetilgang")
    when (sjekkSkrivetilgang(sakId, enhetNr)) {
        true -> {
            application.log.debug("Har skrivetilgang, fortsetter")
            onSuccess()
        }
        false -> {
            application.log.debug("Mangler skrivetilgang, avviser forespørselen")
            call.respond(HttpStatusCode.Forbidden)
        }
    }
}

suspend inline fun PipelineContext<*, ApplicationCall>.kunSaksbehandlerMedSkrivetilgang(
    sakId: no.nav.etterlatte.libs.common.sak.SakId? = null,
    enhetNr: String? = null,
    onSuccess: (Saksbehandler) -> Unit,
) {
    application.log.debug("Sjekker skrivetilgang")
    when (val token = brukerTokenInfo) {
        is Saksbehandler -> {
            when (sjekkSkrivetilgang(sakId, enhetNr)) {
                true -> {
                    application.log.debug("Har skrivetilgang, fortsetter")
                    onSuccess(token)
                }
                false -> {
                    application.log.debug("Mangler skrivetilgang, avviser forespørselen")
                    call.respond(HttpStatusCode.Forbidden)
                }
            }
        }

        else -> {
            application.log.debug("Endepunktet er ikke tilgjengeliggjort for systembruker, avviser forespørselen")
            call.respond(HttpStatusCode.Forbidden)
        }
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
