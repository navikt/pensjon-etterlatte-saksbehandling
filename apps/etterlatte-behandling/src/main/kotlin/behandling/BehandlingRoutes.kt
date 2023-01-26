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
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.domain.TilstandException
import no.nav.etterlatte.behandling.domain.toBehandlingSammendrag
import no.nav.etterlatte.behandling.domain.toDetaljertBehandling
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingService
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.behandling.hendelse.LagretHendelse
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerService
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.libs.common.behandling.BehandlingListe
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendrag
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerAarsak
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerRequest
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.accesstoken
import no.nav.etterlatte.libs.ktor.saksbehandler
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

internal fun Route.behandlingRoutes(
    generellBehandlingService: GenerellBehandlingService,
    foerstegangsbehandlingService: FoerstegangsbehandlingService,
    revurderingService: RevurderingService,
    manueltOpphoerService: ManueltOpphoerService
) {
    val logger = application.log
    route("/api/behandling/{behandlingsid}") {
        get {
            val detaljertBehandlingDTO =
                generellBehandlingService.hentDetaljertBehandlingMedTilbehoer(behandlingsId, saksbehandler, accesstoken)
            call.respond(detaljertBehandlingDTO)
        }

        post("/kommerbarnettilgode") {
            val navIdent = navIdentFraToken() ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                "Kunne ikke hente ut navident for vurdering av ytelsen kommer barnet tilgode"
            )
            val body = call.receive<KommerBarnetTilgodeJson>()
            val kommerBarnetTilgode = KommerBarnetTilgode(
                body.svar,
                body.begrunnelse,
                Grunnlagsopplysning.Saksbehandler(navIdent, Instant.now())
            )

            try {
                foerstegangsbehandlingService.lagreKommerBarnetTilgode(behandlingsId, kommerBarnetTilgode)
                call.respond(HttpStatusCode.OK, kommerBarnetTilgode)
            } catch (e: TilstandException.UgyldigtTilstand) {
                call.respond(HttpStatusCode.BadRequest, "Kunne ikke endre på feltet")
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
            if (!body.isValid()) {
                return@post call.respond(HttpStatusCode.BadRequest)
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
                        it.sak,
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

        route("/{behandlingsid}") {
            get {
                logger.info("Henter detaljert behandling for behandling med id=$behandlingsId")
                when (val behandling = generellBehandlingService.hentDetaljertBehandling(behandlingsId)) {
                    is DetaljertBehandling -> call.respond(behandling)
                    else -> call.respond(HttpStatusCode.NotFound, "Fant ikke behandling med id=$behandlingsId")
                }
            }

            route("/manueltopphoer") {
                get {
                    logger.info("Henter manuelt opphør oppsummering for manuelt opphør med id=$behandlingsId")
                    when (
                        val opphoer =
                            manueltOpphoerService.hentManueltOpphoerOgAlleIverksatteBehandlingerISak(behandlingsId)
                    ) {
                        null -> call.respond(
                            HttpStatusCode.NotFound,
                            "Fant ikke manuelt opphør med id=$behandlingsId"
                        )

                        else -> {
                            val (opphoer, andre) = opphoer
                            call.respond(
                                opphoer.toManueltOpphoerOppsummmering(andre.map { it.toDetaljertBehandling() })
                            )
                        }
                    }
                }
            }

            route("/hendelser") {
                route("/vedtak") {
                    get {
                        call.respond(
                            LagretHendelser(generellBehandlingService.hentHendelserIBehandling(behandlingsId))
                        )
                    }

                    post("/{hendelse}") {
                        val body = call.receive<VedtakHendelse>()
                        generellBehandlingService.registrerVedtakHendelse(
                            behandlingsId,
                            body.vedtakId,
                            requireNotNull(call.parameters["hendelse"]).let { HendelseType.valueOf(it) },
                            body.inntruffet,
                            body.saksbehandler,
                            body.kommentar,
                            body.valgtBegrunnelse
                        )
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

            post("/gyldigfremsatt") {
                val body = call.receive<GyldighetsResultat>()
                foerstegangsbehandlingService.lagreGyldighetsprøving(behandlingsId, body)
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

        route("/foerstegangsbehandling") {
            get {
                call.respond(
                    foerstegangsbehandlingService.hentFoerstegangsbehandling(behandlingsId)?.toDetaljertBehandling()
                        ?: HttpStatusCode.NotFound
                )
            }

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
                            sak = it.sak,
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
            try {
                val manueltOpphoer = manueltOpphoerService.opprettManueltOpphoer(manueltOpphoerRequest)
                if (manueltOpphoer == null) {
                    logger.info(
                        "Sak ${manueltOpphoerRequest.sak} hadde ikke gyldig status for manuelt opphør, så" +
                            "ingen manuelt opphør blir opprettet"
                    )
                    call.respond(HttpStatusCode.Forbidden)
                    return@post
                }
                logger.info(
                    "Manuelt opphør for sak ${manueltOpphoerRequest.sak} er opprettet med behandlingId " +
                        "${manueltOpphoer.id}"
                )
                call.respond(ManueltOpphoerResponse(behandlingId = manueltOpphoer.id.toString()))
            } catch (e: Exception) {
                logger.error("Fikk en feil under oppretting av et manuelt opphør.", e)
                call.respond(
                    HttpStatusCode.InternalServerError
                )
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
                        sak = it.sak,
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

    post("/saker/{sakid}/hendelse/grunnlagendret") { // Søk
        generellBehandlingService.grunnlagISakEndret(sakId)
        call.respond(HttpStatusCode.OK)
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

inline val PipelineContext<*, ApplicationCall>.behandlingsId: UUID
    get() = call.parameters["behandlingsid"]?.let { UUID.fromString(it) } ?: throw NullPointerException(
        "BehandlingsId er ikke i path params"
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
        LocalDate.ofInstant(Instant.parse(_dato), norskTidssone).let {
            YearMonth.of(it.year, it.month)
        }
    } catch (e: Exception) {
        throw RuntimeException("Kunne ikke lese dato for virkningstidspunkt: $_dato", e)
    }

    fun isValid() = dato.year in (0..9999) && begrunnelse != null
}

internal data class FastsettVirkningstidspunktResponse(
    val dato: YearMonth,
    val kilde: Grunnlagsopplysning.Saksbehandler
) {
    companion object {
        fun from(virkningstidspunkt: Virkningstidspunkt) = FastsettVirkningstidspunktResponse(
            virkningstidspunkt.dato,
            virkningstidspunkt.kilde
        )
    }
}

internal data class KommerBarnetTilgodeJson(val svar: JaNeiVetIkke, val begrunnelse: String)