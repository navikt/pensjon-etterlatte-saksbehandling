package no.nav.etterlatte.behandling.aktivitetsplikt

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.LagreAktivitetspliktAktivitetsgrad
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.LagreAktivitetspliktUnntak
import no.nav.etterlatte.behandling.domain.TilstandException
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.revurdering.AutomatiskRevurderingService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.OpprettAktivitetspliktOppfolging
import no.nav.etterlatte.libs.common.behandling.OpprettRevurderingForAktivitetspliktDto
import no.nav.etterlatte.libs.common.behandling.OpprettRevurderingForAktivitetspliktResponse
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.OPPGAVEID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.hentNavidentFraToken
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.oppgaveId
import no.nav.etterlatte.libs.ktor.route.routeLogger
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang
import java.util.UUID

const val AKTIVITET_ID_CALL_PARAMETER = "id"
inline val PipelineContext<*, ApplicationCall>.aktivitetId: UUID
    get() =
        call.parameters[AKTIVITET_ID_CALL_PARAMETER].let { UUID.fromString(it) } ?: throw NullPointerException(
            "Aktivitet id er ikke i path params",
        )

internal fun Route.aktivitetspliktRoutes(
    aktivitetspliktService: AktivitetspliktService,
    behandlingService: BehandlingService,
    grunnlagKlient: GrunnlagKlient,
    automatiskRevurderingService: AutomatiskRevurderingService,
) {
    val logger = routeLogger

    route("/api/behandling/{$BEHANDLINGID_CALL_PARAMETER}/aktivitetsplikt") {
        get {
            val result = aktivitetspliktService.hentAktivitetspliktOppfolging(behandlingId)
            call.respond(result ?: HttpStatusCode.NoContent)
        }

        post {
            kunSkrivetilgang {
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

        route("/aktivitet") {
            get {
                logger.info("Henter aktiviteter for behandlingId=$behandlingId")
                call.respond(aktivitetspliktService.hentAktiviteter(behandlingId))
            }

            post {
                kunSkrivetilgang {
                    logger.info("Oppretter eller oppdaterer aktivitet for behandlingId=$behandlingId")
                    val aktivitet = call.receive<LagreAktivitetspliktAktivitet>()

                    aktivitetspliktService.upsertAktivitet(behandlingId, aktivitet, brukerTokenInfo)

                    call.respond(aktivitetspliktService.hentAktiviteter(behandlingId))
                }
            }

            route("/{$AKTIVITET_ID_CALL_PARAMETER}") {
                delete {
                    kunSkrivetilgang {
                        logger.info("Sletter aktivitet $aktivitetId for behandlingId $behandlingId")

                        aktivitetspliktService.slettAktivitet(behandlingId, aktivitetId)

                        call.respond(aktivitetspliktService.hentAktiviteter(behandlingId))
                    }
                }
            }
        }
    }

    route("/api/sak/{$SAKID_CALL_PARAMETER}/aktivitetsplikt/revurdering") {
        post {
            kunSystembruker {
                logger.info("Sjekker om sak $sakId trenger en ny revurdering etter 6 måneder")
                val request = call.receive<OpprettRevurderingForAktivitetspliktDto>()
                val forrigeBehandling =
                    inTransaction {
                        requireNotNull(behandlingService.hentSisteIverksatte(sakId)) {
                            "Fant ikke forrige behandling i sak $sakId"
                        }
                    }
                val persongalleri =
                    requireNotNull(
                        grunnlagKlient.hentPersongalleri(
                            forrigeBehandling.id,
                            Systembruker.automatiskJobb,
                        )?.opplysning,
                    ) {
                        "Fant ikke persongalleri for behandling ${forrigeBehandling.id}"
                    }

                val aktivitetspliktDato = request.behandlingsmaaned.atDay(1)
                val oppfyllerAktivitetsplikt = aktivitetspliktService.oppfyllerAktivitetsplikt(sakId, aktivitetspliktDato)

                if (!oppfyllerAktivitetsplikt) {
                    inTransaction {
                        automatiskRevurderingService.opprettAutomatiskRevurdering(
                            sakId = sakId,
                            forrigeBehandling = forrigeBehandling,
                            revurderingAarsak = Revurderingaarsak.AKTIVITETSPLIKT,
                            virkningstidspunkt = aktivitetspliktDato,
                            kilde = Vedtaksloesning.GJENNY,
                            persongalleri = persongalleri,
                            frist = request.frist,
                            begrunnelse = "Vurdering av aktivitetsplikt ved 6mnd",
                        ).oppdater()
                    }
                        .let {
                            call.respond(
                                OpprettRevurderingForAktivitetspliktResponse(
                                    opprettetRevurdering = true,
                                    nyBehandlingId = it.id,
                                    forrigeBehandlingId = forrigeBehandling.id,
                                ),
                            )
                        }
                }

                call.respond(
                    OpprettRevurderingForAktivitetspliktResponse(
                        opprettetRevurdering = false,
                        nyBehandlingId = null,
                        forrigeBehandlingId = forrigeBehandling.id,
                    ),
                )
            }
        }
    }

    route("/api/sak/{$SAKID_CALL_PARAMETER}/oppgave/{$OPPGAVEID_CALL_PARAMETER}/aktivitetsplikt/vurdering") {
        get {
            logger.info("Henter aktivitetsplikt vurdering for oppgaveId=$oppgaveId")
            call.respond(aktivitetspliktService.hentVurderingForOppgave(oppgaveId) ?: HttpStatusCode.NotFound)
        }

        post("/aktivitetsgrad") {
            kunSkrivetilgang {
                logger.info("Oppretter aktivitetsgrad for sakId=$sakId og oppgaveId=$oppgaveId")
                aktivitetspliktService.opprettAktivitetsgradForOppgave(
                    aktivitetsgrad = call.receive<LagreAktivitetspliktAktivitetsgrad>(),
                    oppgaveId = oppgaveId,
                    sakId = sakId,
                    brukerTokenInfo = brukerTokenInfo,
                )
                call.respond(HttpStatusCode.Created)
            }
        }

        post("/unntak") {
            kunSkrivetilgang {
                logger.info("Oppretter unntak for sakId=$sakId og oppgaveId=$oppgaveId")
                aktivitetspliktService.opprettUnntakForOpppgave(
                    unntak = call.receive<LagreAktivitetspliktUnntak>(),
                    oppgaveId = oppgaveId,
                    sakId = sakId,
                    brukerTokenInfo = brukerTokenInfo,
                )
                call.respond(HttpStatusCode.Created)
            }
        }
    }

    route("/api/sak/{$SAKID_CALL_PARAMETER}/behandling/{$BEHANDLINGID_CALL_PARAMETER}/aktivitetsplikt/vurdering") {
        get {
            logger.info("Henter aktivitetsplikt vurdering for behandlingId=$behandlingId")
            call.respond(aktivitetspliktService.hentVurderingForBehandling(behandlingId) ?: HttpStatusCode.NotFound)
        }

        post("/aktivitetsgrad") {
            kunSkrivetilgang {
                logger.info("Oppretter aktivitetsgrad for sakId=$sakId og behandlingId=$behandlingId")
                aktivitetspliktService.opprettAktivitetsgradForBehandling(
                    aktivitetsgrad = call.receive<LagreAktivitetspliktAktivitetsgrad>(),
                    behandlingId = behandlingId,
                    sakId = sakId,
                    brukerTokenInfo = brukerTokenInfo,
                )
                call.respond(HttpStatusCode.Created)
            }
        }

        post("/unntak") {
            kunSkrivetilgang {
                logger.info("Oppretter unntak for sakId=$sakId og behandlingId=$behandlingId")
                aktivitetspliktService.opprettUnntakForBehandling(
                    unntak = call.receive<LagreAktivitetspliktUnntak>(),
                    behandlingId = behandlingId,
                    sakId = sakId,
                    brukerTokenInfo = brukerTokenInfo,
                )
                call.respond(HttpStatusCode.Created)
            }
        }
    }
}
