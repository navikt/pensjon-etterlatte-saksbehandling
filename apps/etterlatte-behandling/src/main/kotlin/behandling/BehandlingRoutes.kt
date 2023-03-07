package no.nav.etterlatte.behandling

import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.ManueltOpphoer
import no.nav.etterlatte.behandling.domain.Regulering
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.domain.TilstandException
import no.nav.etterlatte.behandling.domain.toBehandlingSammendrag
import no.nav.etterlatte.behandling.domain.toDetaljertBehandling
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingService
import no.nav.etterlatte.behandling.hendelse.LagretHendelse
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerService
import no.nav.etterlatte.behandling.regulering.RevurderingService
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandling.BehandlingListe
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendrag
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerAarsak
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerRequest
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.behandlingsId
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeNorskTid
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.bruker
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.time.YearMonth
import java.util.*

internal fun Route.behandlingRoutes(
    generellBehandlingService: GenerellBehandlingService,
    foerstegangsbehandlingService: FoerstegangsbehandlingService,
    revurderingService: RevurderingService,
    manueltOpphoerService: ManueltOpphoerService
) {
    val logger = application.log
    route("/api/behandling/{$BEHANDLINGSID_CALL_PARAMETER}") {
        get {
            val detaljertBehandlingDTO =
                generellBehandlingService.hentDetaljertBehandlingMedTilbehoer(behandlingsId, bruker)
            call.respond(detaljertBehandlingDTO)
        }

        post("/gyldigfremsatt") {
            val navIdent = navIdentFraToken() ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                "Kunne ikke hente ut navident for vurdering av ytelsen søkand gyldig framsatt"
            )
            val body = call.receive<VurderingMedBegrunnelseJson>()
            val lagretGyldighetsResultat = foerstegangsbehandlingService.lagreGyldighetsproeving(
                behandlingsId,
                navIdent,
                body.svar,
                body.begrunnelse
            )
            call.respond(HttpStatusCode.OK, lagretGyldighetsResultat)
        }

        post("/kommerbarnettilgode") {
            val navIdent = navIdentFraToken() ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                "Kunne ikke hente ut navident for vurdering av ytelsen kommer barnet tilgode"
            )
            val body = call.receive<VurderingMedBegrunnelseJson>()
            val kommerBarnetTilgode = KommerBarnetTilgode(
                body.svar,
                body.begrunnelse,
                Grunnlagsopplysning.Saksbehandler.create(navIdent)
            )

            try {
                foerstegangsbehandlingService.lagreKommerBarnetTilgode(behandlingsId, kommerBarnetTilgode)
                call.respond(HttpStatusCode.OK, kommerBarnetTilgode)
            } catch (e: TilstandException.UgyldigtTilstand) {
                call.respond(HttpStatusCode.BadRequest, "Kunne ikke endre på feltet")
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
                            opphoer.toManueltOpphoerOppsummmering(andre.map { it.toDetaljertBehandling() })
                        )
                    }
                }
            }
        }

        post("/avbryt") {
            val navIdent = navIdentFraToken() ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                "Kunne ikke hente ut navident for den som vil avbryte"
            )
            generellBehandlingService.avbrytBehandling(behandlingsId, navIdent)
            call.respond(HttpStatusCode.OK)
        }

        post("/virkningstidspunkt") {
            logger.debug("Prøver å fastsette virkningstidspunkt")
            val navIdent = navIdentFraToken() ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                "Kunne ikke hente ut navident for fastsetting av virkningstidspunkt"
            )
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
                val virkningstidspunkt = foerstegangsbehandlingService.lagreVirkningstidspunkt(
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
            } catch (e: TilstandException.UgyldigtTilstand) {
                call.respond(HttpStatusCode.BadRequest, "Kan ikke endre feltet")
            }
        }
    }

    route("/behandlinger") {
        get { // TODO kan slettes? finner ingen spor i loggen.
            call.respond(
                generellBehandlingService.hentBehandlinger().map {
                    BehandlingSammendrag(
                        it.id,
                        it.sak.id,
                        it.status,
                        if (it is Foerstegangsbehandling) it.soeknadMottattDato else it.behandlingOpprettet,
                        it.behandlingOpprettet,
                        it.type,
                        if (it is Revurdering) it.revurderingsaarsak.name else "SOEKNAD",
                        it.virkningstidspunkt,
                        it.vilkaarUtfall
                    )
                }.let { BehandlingListe(it) }
            )
        }

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

        route("/sak") {
            get("/{sakid}") {
                call.respond(
                    generellBehandlingService.hentBehandlingerISak(sakId).map {
                        it.toBehandlingSammendrag()
                    }.let { BehandlingListe(it) }
                )
            }

            delete("/{sakid}") {
                generellBehandlingService.slettBehandlingerISak(sakId)
                call.respond(HttpStatusCode.OK)
            }
        }

        route("/foerstegangsbehandling/{$BEHANDLINGSID_CALL_PARAMETER}") {
            get {
                call.respond(
                    foerstegangsbehandlingService.hentFoerstegangsbehandling(behandlingsId)?.toDetaljertBehandling()
                        ?: HttpStatusCode.NotFound
                )
            }
        }

        route("/foerstegangsbehandling") {
            post {
                val behandlingsBehov = call.receive<BehandlingsBehov>()

                foerstegangsbehandlingService.startFoerstegangsbehandling(
                    behandlingsBehov.sak,
                    behandlingsBehov.persongalleri,
                    behandlingsBehov.mottattDato
                ).also { call.respondText(it.id.toString()) }
            }
        }

        route("/revurdering") {
            get {
                call.respond(
                    revurderingService.hentRevurdering(behandlingsId)?.let {
                        DetaljertBehandling(
                            id = it.id,
                            sak = it.sak.id,
                            sakType = it.sak.sakType,
                            behandlingOpprettet = it.behandlingOpprettet,
                            sistEndret = it.sistEndret,
                            soeknadMottattDato = it.behandlingOpprettet,
                            innsender = it.persongalleri.innsender,
                            soeker = it.persongalleri.soeker,
                            gjenlevende = it.persongalleri.gjenlevende,
                            avdoed = it.persongalleri.avdoed,
                            soesken = it.persongalleri.soesken,
                            gyldighetsproeving = null,
                            status = it.status,
                            virkningstidspunkt = null,
                            behandlingType = BehandlingType.REVURDERING,
                            kommerBarnetTilgode = it.kommerBarnetTilgode,
                            revurderingsaarsak = it.revurderingsaarsak
                        )
                    } ?: HttpStatusCode.NotFound
                )
            }
        }

        route("/manueltopphoer") {
            get {
                call.respond(
                    manueltOpphoerService.hentManueltOpphoerInTransaction(behandlingsId)?.toDetaljertBehandling()
                        ?: HttpStatusCode.NotFound
                )
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

// TODO: fases ut -> nytt endepunkt: /behandlinger/sak/{sakid}
    route("/sak") {
        get("/{sakid}/behandlinger") {
            call.respond(
                generellBehandlingService.hentBehandlingerISak(sakId).map {
                    BehandlingSammendrag(
                        id = it.id,
                        sak = it.sak.id,
                        status = it.status,
                        soeknadMottattDato = if (it is Foerstegangsbehandling) {
                            it.soeknadMottattDato
                        } else {
                            it.behandlingOpprettet
                        },
                        behandlingOpprettet = it.behandlingOpprettet,
                        behandlingType = it.type,
                        aarsak = when (it) {
                            is Foerstegangsbehandling -> "SOEKNAD"
                            is Revurdering -> it.revurderingsaarsak.name
                            is ManueltOpphoer -> "MANUELT OPPHOER"
                            is Regulering -> "REGULERING"
                        },
                        virkningstidspunkt = it.virkningstidspunkt,
                        vilkaarsvurderingUtfall = it.vilkaarUtfall
                    )
                }.let { BehandlingListe(it) }
            )
        }

        delete("/{sakid}/behandlinger") {
            generellBehandlingService.slettBehandlingerISak(sakId)
            call.respond(HttpStatusCode.OK)
        }
    }
}

data class ManueltOpphoerOppsummeringDto(
    val id: UUID,
    val virkningstidspunkt: Virkningstidspunkt?,
    val persongalleri: Persongalleri,
    val opphoerAarsaker: List<ManueltOpphoerAarsak>,
    val fritekstAarsak: String?,
    val andreBehandlinger: List<DetaljertBehandling>
)

private fun ManueltOpphoer.toManueltOpphoerOppsummmering(
    andreBehandlinger: List<DetaljertBehandling>
): ManueltOpphoerOppsummeringDto =
    ManueltOpphoerOppsummeringDto(
        id = this.id,
        virkningstidspunkt = this.virkningstidspunkt,
        persongalleri = this.persongalleri,
        opphoerAarsaker = this.opphoerAarsaker,
        fritekstAarsak = this.fritekstAarsak,
        andreBehandlinger = andreBehandlinger
    )

inline val PipelineContext<*, ApplicationCall>.sakId
    get() = call.parameters["sakid"]?.toLong() ?: throw NullPointerException(
        "sakid er ikke i path params"
    )

fun PipelineContext<Unit, ApplicationCall>.navIdentFraToken() =
    call.principal<TokenValidationContextPrincipal>()?.context?.firstValidToken?.get()?.jwtTokenClaims?.get("NAVident")
        ?.toString()

data class VedtakHendelse(
    val vedtakId: Long,
    val saksbehandler: String?,
    val inntruffet: Tidspunkt,
    val kommentar: String?,
    val valgtBegrunnelse: String?
)

data class LagretHendelser(
    val hendelser: List<LagretHendelse>
)

data class ManueltOpphoerResponse(val behandlingId: String)

data class VirkningstidspunktRequest(@JsonProperty("dato") private val _dato: String, val begrunnelse: String?) {
    val dato: YearMonth = try {
        Tidspunkt.parse(_dato).toLocalDatetimeNorskTid().let {
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

internal data class VurderingMedBegrunnelseJson(val svar: JaNeiVetIkke, val begrunnelse: String)