package no.nav.etterlatte.behandling

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
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerRequest
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.util.UUID

fun Route.behandlingRoutes(
    generellBehandlingService: GenerellBehandlingService,
    foerstegangsbehandlingService: FoerstegangsbehandlingService,
    revurderingService: RevurderingService,
    manueltOpphoerService: ManueltOpphoerService

) {
    val logger = application.log

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
                generellBehandlingService.hentBehandling(behandlingsId)?.toDetaljertBehandling()
                    ?.let { detaljertBehandling ->
                        call.respond(detaljertBehandling)
                        logger.info("Henter detaljert for behandling: $behandlingsId: $detaljertBehandling")
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
                            it.id,
                            it.sak,
                            it.behandlingOpprettet,
                            it.sistEndret,
                            it.soeknadMottattDato,
                            it.persongalleri.innsender,
                            it.persongalleri.soeker,
                            it.persongalleri.gjenlevende,
                            it.persongalleri.avdoed,
                            it.persongalleri.soesken,
                            it.gyldighetsproeving,
                            it.status,
                            BehandlingType.FØRSTEGANGSBEHANDLING
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
                            it.id,
                            it.sak,
                            it.behandlingOpprettet,
                            it.sistEndret,
                            it.behandlingOpprettet,
                            it.persongalleri.innsender,
                            it.persongalleri.soeker,
                            it.persongalleri.gjenlevende,
                            it.persongalleri.avdoed,
                            it.persongalleri.soesken,
                            null,
                            it.status,
                            BehandlingType.REVURDERING
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
                            it.id,
                            it.sak,
                            it.behandlingOpprettet,
                            it.sistEndret,
                            it.behandlingOpprettet,
                            it.persongalleri.innsender,
                            it.persongalleri.soeker,
                            it.persongalleri.gjenlevende,
                            it.persongalleri.avdoed,
                            it.persongalleri.soesken,
                            null,
                            it.status,
                            BehandlingType.MANUELT_OPPHOER
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