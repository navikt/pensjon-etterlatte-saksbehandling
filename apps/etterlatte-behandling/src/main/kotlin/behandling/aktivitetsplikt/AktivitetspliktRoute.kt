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
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradOgUnntak
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.LagreAktivitetspliktAktivitetsgrad
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.LagreAktivitetspliktUnntak
import no.nav.etterlatte.behandling.domain.TilstandException
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.OpprettAktivitetspliktOppfolging
import no.nav.etterlatte.libs.common.behandling.OpprettOppgaveForAktivitetspliktDto
import no.nav.etterlatte.libs.common.behandling.OpprettRevurderingForAktivitetspliktDto
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.OPPGAVEID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.kunSaksbehandler
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.oppgaveId
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang
import org.slf4j.LoggerFactory
import java.util.UUID

const val AKTIVITET_ID_CALL_PARAMETER = "id"
inline val PipelineContext<*, ApplicationCall>.aktivitetId: UUID
    get() =
        call.parameters[AKTIVITET_ID_CALL_PARAMETER]?.let { UUID.fromString(it) } ?: throw UgyldigForespoerselException(
            "MANGLER_AKTIVITET_ID",
            "Aktivitet id er ikke i path params",
        )

const val AKTIVITETSGRAD_ID_CALL_PARAMETER = "aktivitetsgradId"
inline val PipelineContext<*, ApplicationCall>.aktivitetsgradId: UUID
    get() =
        call.parameters[AKTIVITETSGRAD_ID_CALL_PARAMETER]?.let { UUID.fromString(it) } ?: throw UgyldigForespoerselException(
            "MANGLER_AKTIVITETSGRAD_ID",
            "AktivitetsgradId er ikke i path params",
        )

const val UNNTAK_ID_CALL_PARAMETER = "unntakId"
inline val PipelineContext<*, ApplicationCall>.unntakId: UUID
    get() =
        call.parameters[UNNTAK_ID_CALL_PARAMETER]?.let { UUID.fromString(it) } ?: throw UgyldigForespoerselException(
            "MANGLER_UNNTAK_ID",
            "UnntakId er ikke i path params",
        )

internal fun Route.aktivitetspliktRoutes(
    aktivitetspliktService: AktivitetspliktService,
    aktivitetspliktOppgaveService: AktivitetspliktOppgaveService,
) {
    val logger = LoggerFactory.getLogger("AktivitetspliktRoute")

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
                } catch (_: TilstandException.UgyldigTilstand) {
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
                            aktivitetspliktService.upsertAktivitet(aktivitet, brukerTokenInfo, behandlingId)
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
                                aktivitetspliktService.slettAktivitet(aktivitetId, brukerTokenInfo, behandlingId)
                                aktivitetspliktService.hentAktiviteter(behandlingId)
                            }

                        call.respond(aktiviteter)
                    }
                }
            }
        }
    }

    route("/api/sak/{${SAKID_CALL_PARAMETER}}/aktivitetsplikt") {
        route("aktivitet") {
            get {
                kunSaksbehandler {
                    logger.info("Henter aktiviteter for sak $sakId")
                    val dto =
                        inTransaction {
                            runBlocking {
                                aktivitetspliktService.hentAktiviteter(
                                    sakId = sakId,
                                )
                            }
                        }
                    call.respond(dto)
                }
            }
            post {
                kunSkrivetilgang {
                    logger.info("Oppretter eller oppdaterer aktivitet for sakId=$sakId")
                    val aktivitet = call.receive<LagreAktivitetspliktAktivitet>()

                    val aktiviteter =
                        inTransaction {
                            aktivitetspliktService.upsertAktivitet(aktivitet, brukerTokenInfo, sakId = sakId)
                            aktivitetspliktService.hentAktiviteter(sakId = sakId)
                        }
                    call.respond(aktiviteter)
                }
            }

            route("/{$AKTIVITET_ID_CALL_PARAMETER}") {
                delete {
                    kunSkrivetilgang {
                        logger.info("Sletter aktivitet $aktivitetId for sakId $sakId")

                        val aktiviteter =
                            inTransaction {
                                aktivitetspliktService.slettAktivitet(aktivitetId, brukerTokenInfo, sakId = sakId)
                                aktivitetspliktService.hentAktiviteter(sakId = sakId)
                            }

                        call.respond(aktiviteter)
                    }
                }
            }
        }

        route("statistikk/{$BEHANDLINGID_CALL_PARAMETER}") {
            get {
                kunSystembruker {
                    logger.info("Henter aktivitetspliktDto for statistikk")
                    val dto =
                        inTransaction {
                            runBlocking {
                                aktivitetspliktService.hentAktivitetspliktDto(
                                    sakId,
                                    brukerTokenInfo,
                                    behandlingId,
                                )
                            }
                        }
                    call.respond(dto)
                }
            }
        }

        route("vurdering") {
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

        route("revurdering") {
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
        route("oppgave-oppfoelging") {
            post {
                kunSystembruker {
                    logger.info("Sjekker om sak $sakId ikke har varig unntak og skal ha oppgave om infobrev")
                    val request = call.receive<OpprettOppgaveForAktivitetspliktDto>()
                    val opprettet =
                        inTransaction {
                            aktivitetspliktService.opprettOppgaveHvisIkkeVarigUnntak(request)
                        }
                    call.respond(opprettet)
                }
            }
        }
        route("varigUnntak") {
            post {
                kunSystembruker {
                    logger.info("Sjekker om sak $sakId trenger informasjon om aktivetsplikt - varig unntak etter 6 måneder")
                    val request = call.receive<OpprettOppgaveForAktivitetspliktDto>()
                    val opprettet =
                        inTransaction {
                            aktivitetspliktService.opprettOppgaveHvisVarigUnntak(request)
                        }
                    call.respond(opprettet)
                }
            }
        }
    }

    route("/api/aktivitetsplikt/oppgave/{${OPPGAVEID_CALL_PARAMETER}}") {
        get {
            val oppgaveOgVurdering = inTransaction { aktivitetspliktOppgaveService.hentVurderingForOppgave(oppgaveId) }
            call.respond(oppgaveOgVurdering)
        }
        post("brevdata") {
            val brevdata = call.receive<AktivitetspliktInformasjonBrevdataRequest>()
            val oppdaterBrevdata = inTransaction { aktivitetspliktOppgaveService.lagreBrevdata(oppgaveId, brevdata) }
            call.respond(oppdaterBrevdata)
        }
        post("opprettbrev") {
            val brevId =
                inTransaction {
                    aktivitetspliktOppgaveService.opprettBrevHvisKraveneErOppfyltOgDetIkkeFinnes(oppgaveId = oppgaveId, brukerTokenInfo)
                }
            call.respond(BrevIdDto(brevId))
        }
        post("ferdigstillbrev-og-oppgave") {
            val oppgave = inTransaction { aktivitetspliktOppgaveService.ferdigstillBrevOgOppgave(oppgaveId, brukerTokenInfo) }
            call.respond(oppgave)
        }
    }

    route("/api/sak/{$SAKID_CALL_PARAMETER}/oppgave/{$OPPGAVEID_CALL_PARAMETER}/aktivitetsplikt/vurdering") {
        get {
            logger.info("Henter aktivitetsplikt vurdering for oppgaveId=$oppgaveId")
            val vurdering =
                inTransaction { aktivitetspliktService.hentVurderingForOppgaveGammel(oppgaveId) }
                    ?: throw VurderingIkkeFunnetException(sakId, oppgaveId)
            call.respond(vurdering)
        }

        get("/ny") {
            logger.info("Henter ny aktivitetsplikt vurdering for oppgaveId=$oppgaveId")
            val vurdering =
                inTransaction { aktivitetspliktService.hentVurderingForOppgave(oppgaveId) }
                    ?: throw VurderingIkkeFunnetException(sakId, oppgaveId)
            call.respond(vurdering)
        }

        route("/aktivitetsgrad") {
            post {
                kunSkrivetilgang {
                    logger.info("Oppretter aktivitetsgrad for sakId=$sakId og oppgaveId=$oppgaveId")
                    val aktivitetsgrad = call.receive<LagreAktivitetspliktAktivitetsgrad>()
                    val aktivitetspliktVurdering =
                        inTransaction {
                            aktivitetspliktService.upsertAktivitetsgradForOppgave(
                                aktivitetsgrad = aktivitetsgrad,
                                oppgaveId = oppgaveId,
                                sakId = sakId,
                                brukerTokenInfo = brukerTokenInfo,
                            )
                        }
                    call.respond(aktivitetspliktVurdering)
                }
            }

            delete("{$AKTIVITETSGRAD_ID_CALL_PARAMETER}") {
                kunSkrivetilgang {
                    logger.info("Sletter aktivitetsgrad med id=$aktivitetsgradId for oppgaveId=$oppgaveId i sak=$sakId")
                    val vurdering =
                        inTransaction {
                            aktivitetspliktService.slettAktivitetsgradForOppgave(
                                oppgaveId = oppgaveId,
                                aktivitetsgradId = aktivitetsgradId,
                                sakId = sakId,
                                brukerTokenInfo = brukerTokenInfo,
                            )
                        }
                    call.respond(vurdering)
                }
            }
        }

        post("/aktivitetsgrad-og-unntak") {
            kunSkrivetilgang {
                logger.info("Oppretter aktivitetsgrad og unntak for sakId=$sakId og oppgaveId=$oppgaveId")
                val aktivitetsgradOgUnntak = call.receive<AktivitetspliktAktivitetsgradOgUnntak>()
                val oppgave =
                    inTransaction {
                        aktivitetspliktService.upsertAktivtetsgradOgUnntakForOppgave(
                            aktivitetsgradOgUnntak = aktivitetsgradOgUnntak,
                            oppgaveId = oppgaveId,
                            sakId = sakId,
                            brukerTokenInfo = brukerTokenInfo,
                        )
                    }
                call.respond(oppgave)
            }
        }

        route("/unntak") {
            post {
                kunSkrivetilgang {
                    logger.info("Oppretter unntak for sakId=$sakId og oppgaveId=$oppgaveId")
                    val unntak = call.receive<LagreAktivitetspliktUnntak>()
                    val aktivitetspliktVurdering =
                        inTransaction {
                            aktivitetspliktService.upsertUnntakForOppgave(
                                unntak = unntak,
                                oppgaveId = oppgaveId,
                                sakId = sakId,
                                brukerTokenInfo = brukerTokenInfo,
                            )
                        }
                    call.respond(aktivitetspliktVurdering)
                }
            }

            delete("{$UNNTAK_ID_CALL_PARAMETER}") {
                kunSkrivetilgang {
                    logger.info("Sletter unntak med id=$unntakId for sakId=$sakId og oppgaveId=$oppgaveId")
                    val vurdering =
                        inTransaction {
                            aktivitetspliktService.slettUnntakForOppgave(
                                oppgaveId = oppgaveId,
                                sakId = sakId,
                                unntakId = unntakId,
                                brukerTokenInfo = brukerTokenInfo,
                            )
                        }
                    call.respond(vurdering)
                }
            }
        }
    }

    route("/api/sak/{$SAKID_CALL_PARAMETER}/behandling/{$BEHANDLINGID_CALL_PARAMETER}/aktivitetsplikt/vurdering") {
        get {
            logger.info("Henter aktivitetsplikt vurdering for behandlingId=$behandlingId")
            val vurdering =
                inTransaction { aktivitetspliktService.hentVurderingForBehandlingGammel(behandlingId) }
                    ?: throw VurderingIkkeFunnetException(sakId, behandlingId)
            call.respond(vurdering)
        }

        get("/ny") {
            logger.info("Henter ny aktivitetsplikt vurdering for behandlingId=$behandlingId")
            val vurdering =
                inTransaction { aktivitetspliktService.hentVurderingForBehandling(behandlingId) }
                    ?: throw VurderingIkkeFunnetException(sakId, behandlingId)
            call.respond(vurdering)
        }

        post("/aktivitetsgrad") {
            kunSkrivetilgang {
                logger.info("Oppretter aktivitetsgrad for sakId=$sakId og behandlingId=$behandlingId")
                val aktivitetsgrad = call.receive<LagreAktivitetspliktAktivitetsgrad>()
                val vurderingGammel =
                    inTransaction {
                        aktivitetspliktService.upsertAktivitetsgradForBehandling(
                            aktivitetsgrad = aktivitetsgrad,
                            behandlingId = behandlingId,
                            sakId = sakId,
                            brukerTokenInfo = brukerTokenInfo,
                        )
                    }
                call.respond(vurderingGammel)
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

data class BrevIdDto(
    val brevId: BrevID? = null,
)

class VurderingIkkeFunnetException(
    sakId: SakId,
    referanse: UUID,
) : IkkeFunnetException("VURDERING_IKKE_FUNNET", "Fant ikke vurdering i sak=$sakId med referanse=$referanse")
