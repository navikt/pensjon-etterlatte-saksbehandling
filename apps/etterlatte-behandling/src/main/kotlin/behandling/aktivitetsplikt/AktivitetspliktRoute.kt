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
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.LagreAktivitetspliktAktivitetsgrad
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.LagreAktivitetspliktUnntak
import no.nav.etterlatte.behandling.domain.TilstandException
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.OpprettAktivitetspliktOppfolging
import no.nav.etterlatte.libs.common.behandling.OpprettOppgaveForAktivitetspliktVarigUnntakDto
import no.nav.etterlatte.libs.common.behandling.OpprettRevurderingForAktivitetspliktDto
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.OPPGAVEID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.kunSaksbehandler
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.oppgaveId
import no.nav.etterlatte.libs.ktor.route.routeLogger
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang
import java.util.UUID

const val AKTIVITET_ID_CALL_PARAMETER = "id"
inline val PipelineContext<*, ApplicationCall>.aktivitetId: UUID
    get() =
        call.parameters[AKTIVITET_ID_CALL_PARAMETER].let { UUID.fromString(it) } ?: throw UgyldigForespoerselException(
            "MANGLER_AKTIVITET_ID",
            "Aktivitet id er ikke i path params",
        )

internal fun Route.aktivitetspliktRoutes(
    aktivitetspliktService: AktivitetspliktService,
    featureToggleService: FeatureToggleService,
) {
    val logger = routeLogger

    route("/api/behandling/{$BEHANDLINGID_CALL_PARAMETER}/aktivitetsplikt") {
        get {
            val result = inTransaction { aktivitetspliktService.hentAktivitetspliktOppfolging(behandlingId) }
            call.respond(result ?: HttpStatusCode.NoContent)
        }

        post {
            kunSkrivetilgang {
                val oppfolging = call.receive<OpprettAktivitetspliktOppfolging>()

                try {
                    val result =
                        inTransaction {
                            aktivitetspliktService.lagreAktivitetspliktOppfolging(
                                behandlingId,
                                oppfolging,
                                brukerTokenInfo.ident(),
                            )
                        }
                    call.respond(result)
                } catch (e: TilstandException.UgyldigTilstand) {
                    call.respond(HttpStatusCode.BadRequest, "Kunne ikke endre på feltet")
                }
            }
        }

        route("/aktivitet") {
            get {
                logger.info("Henter aktiviteter for behandlingId=$behandlingId")
                call.respond(inTransaction { aktivitetspliktService.hentAktiviteter(behandlingId) })
            }

            post {
                kunSkrivetilgang {
                    logger.info("Oppretter eller oppdaterer aktivitet for behandlingId=$behandlingId")
                    val aktivitet = call.receive<LagreAktivitetspliktAktivitet>()

                    val aktiviteter =
                        inTransaction {
                            aktivitetspliktService.upsertAktivitet(behandlingId, aktivitet, brukerTokenInfo)
                            aktivitetspliktService.hentAktiviteter(behandlingId)
                        }
                    call.respond(aktiviteter)
                }
            }

            route("/{$AKTIVITET_ID_CALL_PARAMETER}") {
                delete {
                    kunSkrivetilgang {
                        logger.info("Sletter aktivitet $aktivitetId for behandlingId $behandlingId")

                        val aktiviteter =
                            inTransaction {
                                aktivitetspliktService.slettAktivitet(behandlingId, aktivitetId, brukerTokenInfo)
                                aktivitetspliktService.hentAktiviteter(behandlingId)
                            }

                        call.respond(aktiviteter)
                    }
                }
            }
        }
    }

    route("/api/sak/{${SAKID_CALL_PARAMETER}}/aktivitetsplikt/vurdering") {
        get {
            kunSaksbehandler {
                logger.info("Henter aktivitetsvurdering for sak $sakId")
                val dto =
                    inTransaction {
                        runBlocking {
                            aktivitetspliktService.hentVurderingForSak(
                                sakId = sakId,
                            )
                        }
                    }
                call.respond(dto)
            }
        }
    }

    route("/api/sak/{$SAKID_CALL_PARAMETER}/aktivitetsplikt/revurdering") {
        post {
            kunSystembruker {
                logger.info("Sjekker om sak $sakId trenger en ny revurdering etter 6 måneder")
                val request = call.receive<OpprettRevurderingForAktivitetspliktDto>()
                val opprettet =
                    inTransaction {
                        aktivitetspliktService.opprettRevurderingHvisKravIkkeOppfylt(
                            request,
                            brukerTokenInfo,
                        )
                    }
                call.respond(opprettet)
            }
        }
    }

    route("/api/sak/{$SAKID_CALL_PARAMETER}/aktivitetsplikt/varigUnntak") {
        post {
            kunSystembruker {
                logger.info("Sjekker om sak $sakId trenger informasjon om aktivetsplikt - varig unntak etter 6 måneder")
                val request = call.receive<OpprettOppgaveForAktivitetspliktVarigUnntakDto>()
                val opprettet =
                    inTransaction {
                        aktivitetspliktService.opprettOppgaveHvisVarigUnntak(request)
                    }
                call.respond(opprettet)
            }
        }
    }

    route("/api/sak/{$SAKID_CALL_PARAMETER}/aktivitetsplikt/vurdering") {
        get {
            logger.info("Henter nyeste aktivitetsplikt vurdering for sakIe=$sakId")
            val vurdering = inTransaction { aktivitetspliktService.hentVurdering(sakId = sakId) }
            call.respond(vurdering ?: HttpStatusCode.NotFound)
        }
    }

    route("/api/sak/{$SAKID_CALL_PARAMETER}/oppgave/{$OPPGAVEID_CALL_PARAMETER}/aktivitetsplikt/vurdering") {
        get {
            logger.info("Henter aktivitetsplikt vurdering for oppgaveId=$oppgaveId")
            val vurdering = inTransaction { aktivitetspliktService.hentVurderingForOppgaveGammel(oppgaveId) }
            call.respond(vurdering ?: HttpStatusCode.NotFound)
        }

        get("/ny") {
            logger.info("Henter ny aktivitetsplikt vurdering for oppgaveId=$oppgaveId")
            val vurdering = inTransaction { aktivitetspliktService.hentVurderingForOppgave(oppgaveId) }
            call.respond(vurdering ?: HttpStatusCode.NotFound)
        }

        post("/aktivitetsgrad") {
            kunSkrivetilgang {
                logger.info("Oppretter aktivitetsgrad for sakId=$sakId og oppgaveId=$oppgaveId")
                val aktivitetsgrad = call.receive<LagreAktivitetspliktAktivitetsgrad>()
                inTransaction {
                    aktivitetspliktService.opprettAktivitetsgradForOppgave(
                        aktivitetsgrad = aktivitetsgrad,
                        oppgaveId = oppgaveId,
                        sakId = sakId,
                        brukerTokenInfo = brukerTokenInfo,
                    )
                }
                call.respond(HttpStatusCode.Created)
            }
        }

        post("/unntak") {
            kunSkrivetilgang {
                logger.info("Oppretter unntak for sakId=$sakId og oppgaveId=$oppgaveId")
                val unntak = call.receive<LagreAktivitetspliktUnntak>()
                inTransaction {
                    aktivitetspliktService.opprettUnntakForOpppgave(
                        unntak = unntak,
                        oppgaveId = oppgaveId,
                        sakId = sakId,
                        brukerTokenInfo = brukerTokenInfo,
                    )
                }
                call.respond(HttpStatusCode.Created)
            }
        }
    }

    route("/api/sak/{$SAKID_CALL_PARAMETER}/behandling/{$BEHANDLINGID_CALL_PARAMETER}/aktivitetsplikt/vurdering") {
        get {
            logger.info("Henter aktivitetsplikt vurdering for behandlingId=$behandlingId")
            val vurdering = inTransaction { aktivitetspliktService.hentVurderingForBehandlingGammel(behandlingId) }
            call.respond(vurdering ?: HttpStatusCode.NotFound)
        }

        get("/ny") {
            logger.info("Henter ny aktivitetsplikt vurdering for behandlingId=$behandlingId")
            val vurdering = inTransaction { aktivitetspliktService.hentVurderingForBehandling(behandlingId) }
            call.respond(vurdering ?: HttpStatusCode.NotFound)
        }

        post("/aktivitetsgrad") {
            kunSkrivetilgang {
                logger.info("Oppretter aktivitetsgrad for sakId=$sakId og behandlingId=$behandlingId")
                val aktivitetsgrad = call.receive<LagreAktivitetspliktAktivitetsgrad>()
                inTransaction {
                    aktivitetspliktService.upsertAktivitetsgradForBehandling(
                        aktivitetsgrad = aktivitetsgrad,
                        behandlingId = behandlingId,
                        sakId = sakId,
                        brukerTokenInfo = brukerTokenInfo,
                    )
                }
                call.respond(HttpStatusCode.Created)
            }
        }

        post("/unntak") {
            kunSkrivetilgang {
                logger.info("Oppretter unntak for sakId=$sakId og behandlingId=$behandlingId")
                val unntak = call.receive<LagreAktivitetspliktUnntak>()
                inTransaction {
                    aktivitetspliktService.upsertUnntakForBehandling(
                        unntak = unntak,
                        behandlingId = behandlingId,
                        sakId = sakId,
                        brukerTokenInfo = brukerTokenInfo,
                    )
                }
                call.respond(HttpStatusCode.Created)
            }
        }
    }
}
