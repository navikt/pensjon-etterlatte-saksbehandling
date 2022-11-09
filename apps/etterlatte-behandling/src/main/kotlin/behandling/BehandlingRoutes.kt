package no.nav.etterlatte.behandling

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
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingService
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerService
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.libs.common.behandling.BehandlingListe
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendrag
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerRequest
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.time.Instant
import java.time.LocalDate
import java.util.*

fun Route.behandlingRoutes(
    generellBehandlingService: GenerellBehandlingService,
    foerstegangsbehandlingService: FoerstegangsbehandlingService,
    revurderingService: RevurderingService,
    manueltOpphoerService: ManueltOpphoerService
) {
    val logger = application.log

    route("/api/behandling/{behandlingsid}/kommerbarnettilgode") {
        post {
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
            foerstegangsbehandlingService.settKommerBarnetTilgode(
                behandlingsId,
                kommerBarnetTilgode
            )

            call.respond(HttpStatusCode.OK, kommerBarnetTilgode)
        }
    }
    route("/behandlinger") {
        get {
            call.respond(
                generellBehandlingService.hentBehandlinger().map {
                    BehandlingSammendrag(
                        it.id,
                        it.sak,
                        it.status,
                        if (it is Foerstegangsbehandling) it.soeknadMottattDato else it.behandlingOpprettet,
                        it.behandlingOpprettet,
                        it.type,
                        if (it is Revurdering) it.revurderingsaarsak.name else "SOEKNAD"
                    )
                }.let { BehandlingListe(it) }
            )
        }

        route("/{behandlingsid}") {
            get {
                generellBehandlingService.hentDetaljertBehandling(behandlingsId)
                    ?.let { detaljertBehandling ->
                        call.respond(detaljertBehandling)
                        logger.info("Henter detaljert for behandling: $behandlingsId")
                    } ?: call.respond(HttpStatusCode.NotFound)
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

            post("/avbrytbehandling") {
                val navIdent = navIdentFraToken() ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    "Kunne ikke hente ut navident for den som vil avbryte"
                )
                generellBehandlingService.avbrytBehandling(behandlingsId, navIdent)
                call.respond(HttpStatusCode.OK)
            }

            post("/gyldigfremsatt") {
                val body = call.receive<GyldighetsResultat>()
                foerstegangsbehandlingService.lagreGyldighetsprøving(behandlingsId, body)
                call.respond(HttpStatusCode.OK)
            }

            post("/virkningstidspunkt") {
                logger.debug("Prøver å fastsette virkningstidspunkt")
                val navIdent = navIdentFraToken() ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    "Kunne ikke hente ut navident for fastsetting av virkningstidspunkt"
                )
                val body = call.receive<FastsettVirkningstidspunktRequest>()
                if (body.dato.dayOfMonth != 1) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Virkningsdato må være første dagen i måneden"
                    )
                }

                val virkningstidspunkt =
                    foerstegangsbehandlingService.fastsettVirkningstidspunkt(behandlingsId, body.dato, navIdent)

                call.respondText(
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.OK,
                    text = FastsettVirkningstidspunktResponse.from(virkningstidspunkt).toJson()
                )
            }
        }

        route("/sak") {
            get("/{sakid}") {
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
                            }
                        )
                    }.let { BehandlingListe(it) }
                )
            }

            delete("/{sakid}") {
                generellBehandlingService.slettBehandlingerISak(sakId)
                call.respond(HttpStatusCode.OK)
            }
        }

        // TODO: fases ut -> nytt endepunkt: /behandlinger/foerstegangsbehandling
        post {
            val behandlingsBehov = call.receive<BehandlingsBehov>()

            foerstegangsbehandlingService.startFoerstegangsbehandling(
                behandlingsBehov.sak,
                behandlingsBehov.persongalleri,
                behandlingsBehov.mottattDato
            ).also { call.respondText(it.id.toString()) }
        }

        route("/foerstegangsbehandling") {
            get {
                call.respond(
                    foerstegangsbehandlingService.hentBehandling(behandlingsId)?.let {
                        DetaljertBehandling(
                            id = it.id,
                            sak = it.sak,
                            behandlingOpprettet = it.behandlingOpprettet,
                            sistEndret = it.sistEndret,
                            soeknadMottattDato = it.soeknadMottattDato,
                            innsender = it.persongalleri.innsender,
                            soeker = it.persongalleri.soeker,
                            gjenlevende = it.persongalleri.gjenlevende,
                            avdoed = it.persongalleri.avdoed,
                            soesken = it.persongalleri.soesken,
                            gyldighetsproeving = it.gyldighetsproeving,
                            status = it.status,
                            virkningstidspunkt = it.hentVirkningstidspunkt(),
                            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                            kommerBarnetTilgode = it.kommerBarnetTilgode
                        )
                    } ?: HttpStatusCode.NotFound
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

            // TODO: SLETT! DENNE!! DETTE ER KUN TIL TESTING! IKKE KJØR I PROD MED MINDRE DU VIL HA SPARKEN
            route("/{sakid}") {
                delete {
                    revurderingService.slettRevurderingISak(sakId)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }

        route("/manueltopphoer") {
            get {
                call.respond(
                    manueltOpphoerService.hentManueltOpphoerInTransaction(behandlingsId)?.let {
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
                            behandlingType = BehandlingType.MANUELT_OPPHOER,
                            kommerBarnetTilgode = null
                        )
                    } ?: HttpStatusCode.NotFound
                )
            }
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
    }

// TODO: fases ut -> nytt endepunkt: /behandlinger/foerstegangsbehandling
    post {
        val behandlingsBehov = call.receive<BehandlingsBehov>()

        foerstegangsbehandlingService.startFoerstegangsbehandling(
            behandlingsBehov.sak,
            behandlingsBehov.persongalleri,
            behandlingsBehov.mottattDato
        ).also { call.respondText(it.id.toString()) }
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
                        }
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

inline val PipelineContext<*, ApplicationCall>.behandlingsId: UUID
    get() = requireNotNull(call.parameters["behandlingsid"]).let {
        UUID.fromString(
            it
        )
    }
inline val PipelineContext<*, ApplicationCall>.sakId get() = requireNotNull(call.parameters["sakid"]).toLong()
fun PipelineContext<Unit, ApplicationCall>.navIdentFraToken() = call.principal<TokenValidationContextPrincipal>()
    ?.context?.firstValidToken?.get()?.jwtTokenClaims?.get("NAVident")?.toString()

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

enum class HendelseType {
    FATTET, ATTESTERT, UNDERKJENT, VILKAARSVURDERT, BEREGNET, AVKORTET, IVERKSATT
}

data class ManueltOpphoerResponse(val behandlingId: String)

internal data class FastsettVirkningstidspunktRequest(val dato: LocalDate)
internal data class FastsettVirkningstidspunktResponse(
    val dato: LocalDate,
    val kilde: Grunnlagsopplysning.Saksbehandler
) {
    companion object {
        fun from(virkningstidspunkt: Virkningstidspunkt) = FastsettVirkningstidspunktResponse(
            LocalDate.of(virkningstidspunkt.dato.year, virkningstidspunkt.dato.month, 1),
            virkningstidspunkt.kilde
        )
    }
}

internal data class KommerBarnetTilgodeJson(val svar: JaNeiVetIkke, val begrunnelse: String)