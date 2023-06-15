package no.nav.etterlatte.behandling

import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.domain.ManueltOpphoer
import no.nav.etterlatte.behandling.domain.TilstandException
import no.nav.etterlatte.behandling.domain.toBehandlingSammendrag
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingService
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerAarsak
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerRequest
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerService
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Utenlandstilsnitt
import no.nav.etterlatte.libs.common.behandling.UtenlandstilsnittType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.behandlingsId
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.hentNavidentFraToken
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.bruker
import java.time.YearMonth
import java.util.*

internal fun Route.behandlingRoutes(
    generellBehandlingService: GenerellBehandlingService,
    foerstegangsbehandlingService: FoerstegangsbehandlingService,
    manueltOpphoerService: ManueltOpphoerService
) {
    val logger = application.log
    route("/api/behandling/{$BEHANDLINGSID_CALL_PARAMETER}/") {
        get {
            val detaljertBehandlingDTO =
                generellBehandlingService.hentDetaljertBehandlingMedTilbehoer(behandlingsId, bruker)
            call.respond(detaljertBehandlingDTO)
        }

        post("/gyldigfremsatt") {
            hentNavidentFraToken { navIdent ->
                val body = call.receive<JaNeiMedBegrunnelse>()
                when (
                    val lagretGyldighetsResultat = foerstegangsbehandlingService.lagreGyldighetsproeving(
                        behandlingsId,
                        navIdent,
                        body
                    )
                ) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respond(HttpStatusCode.OK, lagretGyldighetsResultat)
                }
            }
        }

        post("/kommerbarnettilgode") {
            hentNavidentFraToken { navIdent ->
                val body = call.receive<JaNeiMedBegrunnelse>()
                val kommerBarnetTilgode = KommerBarnetTilgode(
                    body,
                    Grunnlagsopplysning.Saksbehandler.create(navIdent)
                )

                try {
                    foerstegangsbehandlingService.lagreKommerBarnetTilgode(behandlingsId, kommerBarnetTilgode)
                    call.respond(HttpStatusCode.OK, kommerBarnetTilgode)
                } catch (e: TilstandException.UgyldigTilstand) {
                    call.respond(HttpStatusCode.BadRequest, "Kunne ikke endre på feltet")
                }
            }
        }

        route("/manueltopphoer") {
            get {
                logger.info("Henter manuelt opphør oppsummering for manuelt opphør med id=$behandlingsId")
                when (
                    val opphoerOgBehandlinger =
                        manueltOpphoerService.hentManueltOpphoerOgAlleIverksatteBehandlingerISak(behandlingsId)
                ) {
                    null -> call.respond(
                        HttpStatusCode.NotFound,
                        "Fant ikke manuelt opphør med id=$behandlingsId"
                    )

                    else -> {
                        val (opphoer, andre) = opphoerOgBehandlinger
                        call.respond(
                            opphoer.toManueltOpphoerOppsummmering(andre.map { it.toBehandlingSammendrag() })
                        )
                    }
                }
            }
        }

        post("/avbryt") {
            hentNavidentFraToken { navIdent ->
                generellBehandlingService.avbrytBehandling(behandlingsId, navIdent)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/virkningstidspunkt") {
            hentNavidentFraToken { navIdent ->
                logger.debug("Prøver å fastsette virkningstidspunkt")
                val body = call.receive<VirkningstidspunktRequest>()

                val erGyldigVirkningstidspunkt = generellBehandlingService.erGyldigVirkningstidspunkt(
                    behandlingsId,
                    bruker,
                    body
                )
                if (!erGyldigVirkningstidspunkt) {
                    return@post call.respond(HttpStatusCode.BadRequest, "Ugyldig virkningstidspunkt")
                }

                try {
                    val virkningstidspunkt = generellBehandlingService.oppdaterVirkningstidspunkt(
                        behandlingsId,
                        body.dato,
                        navIdent,
                        body.begrunnelse!!
                    )

                    call.respondText(
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.OK,
                        text = FastsettVirkningstidspunktResponse.from(virkningstidspunkt).toJson()
                    )
                } catch (e: TilstandException.UgyldigTilstand) {
                    call.respond(HttpStatusCode.BadRequest, "Kan ikke endre feltet")
                }
            }
        }

        post("/utenlandstilsnitt") {
            hentNavidentFraToken { navIdent ->
                logger.debug("Prøver å fastsette utenlandstilsnitt")
                val body = call.receive<UtenlandstilsnittRequest>()

                try {
                    val utenlandstilsnitt = Utenlandstilsnitt(
                        type = body.utenlandstilsnittType,
                        kilde = Grunnlagsopplysning.Saksbehandler.create(navIdent),
                        begrunnelse = body.begrunnelse
                    )

                    generellBehandlingService.oppdaterUtenlandstilsnitt(behandlingsId, utenlandstilsnitt)

                    call.respondText(
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.OK,
                        text = utenlandstilsnitt.toJson()

                    )
                } catch (e: TilstandException.UgyldigTilstand) {
                    call.respond(HttpStatusCode.BadRequest, "Kan ikke endre feltet")
                }
            }
        }

        post("/boddellerarbeidetutlandet") {
            hentNavidentFraToken { navIdent ->
                logger.debug("Prøver å fastsette boddEllerArbeidetUtlandet")
                val body = call.receive<BoddEllerArbeidetUtlandetRequest>()

                try {
                    val boddEllerArbeidetUtlandet = BoddEllerArbeidetUtlandet(
                        boddEllerArbeidetUtlandet = body.boddEllerArbeidetUtlandet,
                        kilde = Grunnlagsopplysning.Saksbehandler.create(navIdent),
                        begrunnelse = body.begrunnelse
                    )

                    generellBehandlingService.oppdaterBoddEllerArbeidetUtlandet(
                        behandlingsId,
                        boddEllerArbeidetUtlandet
                    )

                    call.respondText(
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.OK,
                        text = boddEllerArbeidetUtlandet.toJson()

                    )
                } catch (e: TilstandException.UgyldigTilstand) {
                    call.respond(HttpStatusCode.BadRequest, "Kan ikke endre feltet")
                }
            }
        }
    }

    route("/behandlinger") {
        route("/{$BEHANDLINGSID_CALL_PARAMETER}") {
            get {
                logger.info("Henter detaljert behandling for behandling med id=$behandlingsId")
                when (val behandling = generellBehandlingService.hentDetaljertBehandling(behandlingsId)) {
                    is DetaljertBehandling -> call.respond(behandling)
                    else -> call.respond(HttpStatusCode.NotFound, "Fant ikke behandling med id=$behandlingsId")
                }
            }

            post("/gyldigfremsatt") {
                val body = call.receive<GyldighetsResultat>()
                foerstegangsbehandlingService.lagreGyldighetsproeving(behandlingsId, body)
                call.respond(HttpStatusCode.OK)
            }
        }

        route("/foerstegangsbehandling") {
            post {
                val behandlingsBehov = call.receive<BehandlingsBehov>()

                when (
                    val behandling = foerstegangsbehandlingService.startFoerstegangsbehandling(
                        behandlingsBehov.sak,
                        behandlingsBehov.persongalleri,
                        behandlingsBehov.mottattDato,
                        Vedtaksloesning.GJENNY
                    )
                ) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respondText(behandling.id.toString())
                }
            }
        }
    }

    route("/api/behandlinger/{sakid}/manueltopphoer") {
        post {
            val manueltOpphoerRequest = call.receive<ManueltOpphoerRequest>()
            logger.info("Mottat forespørsel om å gjennomføre et manuelt opphør på sak ${manueltOpphoerRequest.sak}")
            when (val manueltOpphoer = manueltOpphoerService.opprettManueltOpphoer(manueltOpphoerRequest)) {
                is ManueltOpphoer -> {
                    logger.info(
                        "Manuelt opphør for sak ${manueltOpphoerRequest.sak} er opprettet med behandlingId " +
                            "${manueltOpphoer.id}"
                    )
                    call.respond(ManueltOpphoerResponse(behandlingId = manueltOpphoer.id.toString()))
                }

                else -> {
                    logger.info(
                        "Sak ${manueltOpphoerRequest.sak} hadde ikke gyldig status for manuelt opphør, så" +
                            "ingen manuelt opphør blir opprettet"
                    )
                    call.respond(HttpStatusCode.Forbidden)
                }
            }
        }
    }
}

data class UtenlandstilsnittRequest(
    val utenlandstilsnittType: UtenlandstilsnittType,
    val begrunnelse: String
)

data class BoddEllerArbeidetUtlandetRequest(
    val boddEllerArbeidetUtlandet: Boolean,
    val begrunnelse: String
)

data class ManueltOpphoerOppsummeringDto(
    val id: UUID,
    val virkningstidspunkt: Virkningstidspunkt?,
    val opphoerAarsaker: List<ManueltOpphoerAarsak>,
    val fritekstAarsak: String?,
    val andreBehandlinger: List<BehandlingSammendrag>
)

private fun ManueltOpphoer.toManueltOpphoerOppsummmering(
    andreBehandlinger: List<BehandlingSammendrag>
): ManueltOpphoerOppsummeringDto =
    ManueltOpphoerOppsummeringDto(
        id = this.id,
        virkningstidspunkt = this.virkningstidspunkt,
        opphoerAarsaker = this.opphoerAarsaker,
        fritekstAarsak = this.fritekstAarsak,
        andreBehandlinger = andreBehandlinger
    )

data class VedtakHendelse(
    val vedtakId: Long,
    val saksbehandler: String?,
    val inntruffet: Tidspunkt,
    val kommentar: String?,
    val valgtBegrunnelse: String?
)

data class ManueltOpphoerResponse(val behandlingId: String)

data class VirkningstidspunktRequest(@JsonProperty("dato") private val _dato: String, val begrunnelse: String?) {
    val dato: YearMonth = try {
        Tidspunkt.parse(_dato).toNorskTid().let {
            YearMonth.of(it.year, it.month)
        } ?: throw IllegalArgumentException("Dato $_dato må være definert")
    } catch (e: Exception) {
        throw RuntimeException("Kunne ikke lese dato for virkningstidspunkt: $_dato", e)
    }
}

internal data class FastsettVirkningstidspunktResponse(
    val dato: YearMonth,
    val kilde: Grunnlagsopplysning.Saksbehandler,
    val begrunnelse: String
) {
    companion object {
        fun from(virkningstidspunkt: Virkningstidspunkt) = FastsettVirkningstidspunktResponse(
            virkningstidspunkt.dato,
            virkningstidspunkt.kilde,
            virkningstidspunkt.begrunnelse
        )
    }
}