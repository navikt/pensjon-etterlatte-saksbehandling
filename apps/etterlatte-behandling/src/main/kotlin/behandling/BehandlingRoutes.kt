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
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktService
import no.nav.etterlatte.behandling.domain.ManueltOpphoer
import no.nav.etterlatte.behandling.domain.TilstandException
import no.nav.etterlatte.behandling.domain.toBehandlingSammendrag
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeService
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerAarsak
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerRequest
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingsBehov
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.NyBehandlingRequest
import no.nav.etterlatte.libs.common.behandling.OpprettAktivitetspliktOppfolging
import no.nav.etterlatte.libs.common.behandling.Utenlandstilsnitt
import no.nav.etterlatte.libs.common.behandling.UtenlandstilsnittType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.behandlingId
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.OppdaterGrunnlagRequest
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.hentNavidentFraToken
import no.nav.etterlatte.libs.common.kunSaksbehandler
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import java.time.YearMonth
import java.util.UUID

internal fun Route.behandlingRoutes(
    behandlingService: BehandlingService,
    gyldighetsproevingService: GyldighetsproevingService,
    kommerBarnetTilGodeService: KommerBarnetTilGodeService,
    manueltOpphoerService: ManueltOpphoerService,
    aktivitetspliktService: AktivitetspliktService,
    behandlingFactory: BehandlingFactory,
) {
    val logger = application.log

    post("/api/behandling") {
        kunSaksbehandler {
            val request = call.receive<NyBehandlingRequest>()
            val behandling = behandlingFactory.opprettSakOgBehandlingForOppgave(request)
            call.respondText(behandling.id.toString())
        }
    }

    route("/api/behandling/{$BEHANDLINGID_CALL_PARAMETER}/") {
        get {
            val detaljertBehandlingDTO =
                behandlingService.hentDetaljertBehandlingMedTilbehoer(behandlingId, brukerTokenInfo)
            call.respond(detaljertBehandlingDTO)
        }

        post("/gyldigfremsatt") {
            hentNavidentFraToken { navIdent ->
                val body = call.receive<JaNeiMedBegrunnelse>()
                when (
                    val lagretGyldighetsResultat =
                        inTransaction {
                            gyldighetsproevingService.lagreGyldighetsproeving(
                                behandlingId,
                                navIdent,
                                body,
                            )
                        }
                ) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respond(HttpStatusCode.OK, lagretGyldighetsResultat)
                }
            }
        }

        post("/kommerbarnettilgode") {
            hentNavidentFraToken { navIdent ->
                val body = call.receive<JaNeiMedBegrunnelse>()
                val kommerBarnetTilgode =
                    KommerBarnetTilgode(
                        body.svar,
                        body.begrunnelse,
                        Grunnlagsopplysning.Saksbehandler.create(navIdent),
                        behandlingId,
                    )

                try {
                    inTransaction { kommerBarnetTilGodeService.lagreKommerBarnetTilgode(kommerBarnetTilgode) }
                    call.respond(HttpStatusCode.OK, kommerBarnetTilgode)
                } catch (e: TilstandException.UgyldigTilstand) {
                    call.respond(HttpStatusCode.BadRequest, "Kunne ikke endre på feltet")
                }
            }
        }

        route("/manueltopphoer") {
            get {
                logger.info("Henter manuelt opphør oppsummering for manuelt opphør med id=$behandlingId")
                when (
                    val opphoerOgBehandlinger =
                        manueltOpphoerService.hentManueltOpphoerOgAlleIverksatteBehandlingerISak(behandlingId)
                ) {
                    null ->
                        call.respond(
                            HttpStatusCode.NotFound,
                            "Fant ikke manuelt opphør med id=$behandlingId",
                        )

                    else -> {
                        val (opphoer, andre) = opphoerOgBehandlinger
                        call.respond(
                            opphoer.toManueltOpphoerOppsummmering(andre.map { it.toBehandlingSammendrag() }),
                        )
                    }
                }
            }
        }

        post("/avbryt") {
            inTransaction { behandlingService.avbrytBehandling(behandlingId, brukerTokenInfo) }
            call.respond(HttpStatusCode.OK)
        }

        post("/virkningstidspunkt") {
            hentNavidentFraToken { navIdent ->
                logger.debug("Prøver å fastsette virkningstidspunkt")
                val body = call.receive<VirkningstidspunktRequest>()

                val erGyldigVirkningstidspunkt =
                    behandlingService.erGyldigVirkningstidspunkt(
                        behandlingId,
                        brukerTokenInfo,
                        body,
                    )
                if (!erGyldigVirkningstidspunkt) {
                    return@post call.respond(HttpStatusCode.BadRequest, "Ugyldig virkningstidspunkt")
                }

                try {
                    val virkningstidspunkt =
                        inTransaction {
                            behandlingService.oppdaterVirkningstidspunkt(
                                behandlingId,
                                body.dato,
                                navIdent,
                                body.begrunnelse!!,
                            )
                        }

                    call.respondText(
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.OK,
                        text = FastsettVirkningstidspunktResponse.from(virkningstidspunkt).toJson(),
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
                    val utenlandstilsnitt =
                        Utenlandstilsnitt(
                            type = body.utenlandstilsnittType,
                            kilde = Grunnlagsopplysning.Saksbehandler.create(navIdent),
                            begrunnelse = body.begrunnelse,
                        )

                    inTransaction {
                        behandlingService.oppdaterUtenlandstilsnitt(behandlingId, utenlandstilsnitt)
                    }

                    call.respondText(
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.OK,
                        text = utenlandstilsnitt.toJson(),
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
                    val boddEllerArbeidetUtlandet =
                        BoddEllerArbeidetUtlandet(
                            boddEllerArbeidetUtlandet = body.boddEllerArbeidetUtlandet,
                            kilde = Grunnlagsopplysning.Saksbehandler.create(navIdent),
                            begrunnelse = body.begrunnelse,
                            boddArbeidetIkkeEosEllerAvtaleland = body.boddArbeidetIkkeEosEllerAvtaleland,
                            boddArbeidetEosNordiskKonvensjon = body.boddArbeidetEosNordiskKonvensjon,
                            boddArbeidetAvtaleland = body.boddArbeidetAvtaleland,
                            vurdereAvoededsTrygdeavtale = body.vurdereAvoededsTrygdeavtale,
                            norgeErBehandlendeland = body.norgeErBehandlendeland,
                            skalSendeKravpakke = body.skalSendeKravpakke,
                        )

                    inTransaction {
                        behandlingService.oppdaterBoddEllerArbeidetUtlandet(
                            behandlingId,
                            boddEllerArbeidetUtlandet,
                        )
                    }

                    call.respondText(
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.OK,
                        text = boddEllerArbeidetUtlandet.toJson(),
                    )
                } catch (e: TilstandException.UgyldigTilstand) {
                    call.respond(HttpStatusCode.BadRequest, "Kan ikke endre feltet")
                }
            }
        }

        route("/aktivitetsplikt") {
            get {
                val result = aktivitetspliktService.hentAktivitetspliktOppfolging(behandlingId)
                call.respond(result ?: HttpStatusCode.NoContent)
            }

            post {
                hentNavidentFraToken { navIdent ->
                    val oppfolging = call.receive<OpprettAktivitetspliktOppfolging>()

                    try {
                        val result =
                            aktivitetspliktService.lagreAktivitetspliktOppfolging(
                                behandlingId,
                                oppfolging,
                                navIdent,
                            )
                        call.respond(result)
                    } catch (e: TilstandException.UgyldigTilstand) {
                        call.respond(HttpStatusCode.BadRequest, "Kunne ikke endre på feltet")
                    }
                }
            }
        }

        route("/oppdater-grunnlag") {
            post {
                val request = call.receive<OppdaterGrunnlagRequest>()
                inTransaction {
                    behandlingService.oppdaterGrunnlag(
                        behandlingsId,
                        request.sakId,
                        request.sakType,
                    )
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    route("/behandlinger") {
        route("/{$BEHANDLINGID_CALL_PARAMETER}") {
            get {
                logger.info("Henter detaljert behandling for behandling med id=$behandlingId")
                when (val behandling = behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo)) {
                    is DetaljertBehandling -> call.respond(behandling)
                    else -> call.respond(HttpStatusCode.NotFound, "Fant ikke behandling med id=$behandlingId")
                }
            }

            post("/gyldigfremsatt") {
                val body = call.receive<GyldighetsResultat>()
                gyldighetsproevingService.lagreGyldighetsproeving(behandlingId, body)
                call.respond(HttpStatusCode.OK)
            }
        }

        route("/opprettbehandling") {
            post {
                val behandlingsBehov = call.receive<BehandlingsBehov>()

                when (
                    val behandling =
                        inTransaction {
                            behandlingFactory.opprettBehandling(
                                behandlingsBehov.sakId,
                                behandlingsBehov.persongalleri,
                                behandlingsBehov.mottattDato,
                                Vedtaksloesning.GJENNY,
                            )
                        }?.behandling
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
            logger.info("Mottat forespørsel om å gjennomføre et manuelt opphør på sak ${manueltOpphoerRequest.sakId}")
            when (val manueltOpphoer = manueltOpphoerService.opprettManueltOpphoer(manueltOpphoerRequest)) {
                is ManueltOpphoer -> {
                    logger.info(
                        "Manuelt opphør for sak ${manueltOpphoerRequest.sakId} er opprettet med behandlingId " +
                            "${manueltOpphoer.id}",
                    )
                    call.respond(ManueltOpphoerResponse(behandlingId = manueltOpphoer.id.toString()))
                }

                else -> {
                    logger.info(
                        "Sak ${manueltOpphoerRequest.sakId} hadde ikke gyldig status for manuelt opphør, så" +
                            "ingen manuelt opphør blir opprettet",
                    )
                    call.respond(HttpStatusCode.Forbidden)
                }
            }
        }
    }
}

data class UtenlandstilsnittRequest(
    val utenlandstilsnittType: UtenlandstilsnittType,
    val begrunnelse: String,
)

data class BoddEllerArbeidetUtlandetRequest(
    val boddEllerArbeidetUtlandet: Boolean,
    val begrunnelse: String,
    val boddArbeidetIkkeEosEllerAvtaleland: Boolean? = false,
    val boddArbeidetEosNordiskKonvensjon: Boolean? = false,
    val boddArbeidetAvtaleland: Boolean? = false,
    val vurdereAvoededsTrygdeavtale: Boolean? = false,
    val norgeErBehandlendeland: Boolean? = false,
    val skalSendeKravpakke: Boolean? = false,
)

data class ManueltOpphoerOppsummeringDto(
    val id: UUID,
    val virkningstidspunkt: Virkningstidspunkt?,
    val opphoerAarsaker: List<ManueltOpphoerAarsak>,
    val fritekstAarsak: String?,
    val andreBehandlinger: List<BehandlingSammendrag>,
)

private fun ManueltOpphoer.toManueltOpphoerOppsummmering(andreBehandlinger: List<BehandlingSammendrag>): ManueltOpphoerOppsummeringDto =
    ManueltOpphoerOppsummeringDto(
        id = this.id,
        virkningstidspunkt = this.virkningstidspunkt,
        opphoerAarsaker = this.opphoerAarsaker,
        fritekstAarsak = this.fritekstAarsak,
        andreBehandlinger = andreBehandlinger,
    )

data class ManueltOpphoerResponse(val behandlingId: String)

data class VirkningstidspunktRequest(
    @JsonProperty("dato") private val _dato: String,
    val begrunnelse: String?,
) {
    val dato: YearMonth =
        try {
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
    val begrunnelse: String,
) {
    companion object {
        fun from(virkningstidspunkt: Virkningstidspunkt) =
            FastsettVirkningstidspunktResponse(
                virkningstidspunkt.dato,
                virkningstidspunkt.kilde,
                virkningstidspunkt.begrunnelse,
            )
    }
}
