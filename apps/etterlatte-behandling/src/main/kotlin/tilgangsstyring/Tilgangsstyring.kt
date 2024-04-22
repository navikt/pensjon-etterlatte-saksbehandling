package no.nav.etterlatte.tilgangsstyring

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.Hook
import io.ktor.server.application.RouteScopedPlugin
import io.ktor.server.application.call
import io.ktor.server.application.createRouteScopedPlugin
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
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.libs.ktor.route.CallParamAuthId
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.sak.TilgangService
import org.slf4j.LoggerFactory

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
                val behandlingId = call.parameters[CallParamAuthId.BEHANDLINGID.value]
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

                val sakId = call.parameters[CallParamAuthId.SAKID.value]
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

                val oppgaveId = call.parameters[CallParamAuthId.OPPGAVEID.value]
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

                val klageId = call.parameters[CallParamAuthId.KLAGEID.value]
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

private val logger = LoggerFactory.getLogger("Skrivetilgang")

fun PipelineContext<*, ApplicationCall>.sjekkSkrivetilgang(
    sakId: Long? = null,
    enhetNr: String? = null,
): Boolean {
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

private fun PipelineContext<*, ApplicationCall>.finnSkriveTilgangForId(sakId: Long? = null): String? {
    val sakTilgangDao = Kontekst.get().sakTilgangDao
    if (sakId != null) {
        return sakTilgangDao.hentSakMedGraderingOgSkjerming(sakId)?.enhetNr
    }
    val funnetCallIdParametersType = CallParamAuthId.entries.firstOrNull { call.parameters.contains(it.value) }
    return if (funnetCallIdParametersType == null) {
        logger.warn("Fant ingen pathparam i url: ${call.request.path()} params: ${call.parameters}")
        null
    } else {
        val idForRequest = call.parameters[funnetCallIdParametersType.value]!!
        when (funnetCallIdParametersType) {
            CallParamAuthId.BEHANDLINGID -> sakTilgangDao.hentSakMedGraderingOgSkjermingPaaBehandling(idForRequest)?.enhetNr
            CallParamAuthId.SAKID -> sakTilgangDao.hentSakMedGraderingOgSkjerming(idForRequest.toLong())?.enhetNr
            CallParamAuthId.OPPGAVEID -> sakTilgangDao.hentSakMedGraderingOgSkjermingPaaOppgave(idForRequest)?.enhetNr
            CallParamAuthId.KLAGEID -> sakTilgangDao.hentSakMedGraderingOgSkjermingPaaKlage(idForRequest)?.enhetNr
            CallParamAuthId.GENERELLBEHANDLINGID ->
                sakTilgangDao.hentSakMedGraderingOgSkjermingPaaGenerellbehandling(
                    idForRequest,
                )?.enhetNr
        }
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
