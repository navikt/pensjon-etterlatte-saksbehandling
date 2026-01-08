package no.nav.etterlatte.behandling.aktivitetsplikt

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradOgUnntak
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.LagreAktivitetspliktAktivitetsgrad
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.LagreAktivitetspliktUnntak
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.OpprettOppgaveForAktivitetspliktDto
import no.nav.etterlatte.libs.common.behandling.OpprettRevurderingForAktivitetspliktDto
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
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
const val HENDELSE_ID_CALL_PARAMETER = "hendelseId"
inline val RoutingContext.aktivitetId: UUID
    get() =
        call.parameters[AKTIVITET_ID_CALL_PARAMETER]?.let { UUID.fromString(it) } ?: throw UgyldigForespoerselException(
            "MANGLER_AKTIVITET_ID",
            "Aktivitet id er ikke i path params",
        )

inline val RoutingContext.hendelseId: UUID
    get() =
        call.parameters[HENDELSE_ID_CALL_PARAMETER]?.let { UUID.fromString(it) } ?: throw UgyldigForespoerselException(
            "MANGLER_HENDELSE_ID",
            "Hendelse id er ikke i path params",
        )

const val AKTIVITETSGRAD_ID_CALL_PARAMETER = "aktivitetsgradId"
inline val RoutingContext.aktivitetsgradId: UUID
    get() =
        call.parameters[AKTIVITETSGRAD_ID_CALL_PARAMETER]?.let { UUID.fromString(it) } ?: throw UgyldigForespoerselException(
            "MANGLER_AKTIVITETSGRAD_ID",
            "AktivitetsgradId er ikke i path params",
        )

const val UNNTAK_ID_CALL_PARAMETER = "unntakId"
inline val RoutingContext.unntakId: UUID
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
        /*
            Denne ligger her for å hente ut data som ble vurdert på gammel flyt
         */
        get {
            val result = inTransaction { aktivitetspliktService.hentAktivitetspliktOppfolging(behandlingId) }
            call.respond(result ?: HttpStatusCode.NoContent)
        }

        route("/aktivitet") {
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
                                aktivitetspliktService.slettAktivitet(aktivitetId, behandlingId)
                                aktivitetspliktService.hentAktiviteter(behandlingId)
                            }

                        call.respond(aktiviteter)
                    }
                }
            }
        }
        route("hendelse") {
            post {
                kunSkrivetilgang {
                    logger.info("Oppretter eller oppdaterer hendelser for behandlingId $behandlingId")
                    val hendelse = call.receive<LagreAktivitetspliktHendelse>()
                    val hendelser =
                        inTransaction {
                            aktivitetspliktService.upsertHendelse(hendelse, brukerTokenInfo, behandlingId = behandlingId)
                            aktivitetspliktService.hentHendelser(behandlingId = behandlingId)
                        }
                    call.respond(hendelser)
                }
            }
        }
    }

    route("/api/sak/{${SAKID_CALL_PARAMETER}}/aktivitetsplikt") {
        route("aktivitet") {
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
                                aktivitetspliktService.slettAktivitet(aktivitetId, sakId = sakId)
                                aktivitetspliktService.hentAktiviteter(sakId = sakId)
                            }

                        call.respond(aktiviteter)
                    }
                }
            }
        }

        route("/aktivitet-og-hendelser") {
            get {
                val behandlingId =
                    call.parameters[BEHANDLINGID_CALL_PARAMETER]?.let {
                        UUID.fromString(it)
                    }
                logger.info("Henter aktiviteter og hendelser for sak=$sakId")
                call.respond(inTransaction { aktivitetspliktService.hentAktiviteterHendelser(sakId = sakId, behandlingId = behandlingId) })
            }
        }

        route("hendelse") {
            get {
                kunSaksbehandler {
                    val behandlingId =
                        call.parameters[BEHANDLINGID_CALL_PARAMETER]?.let {
                            UUID.fromString(it)
                        }
                    logger.info("Henter hendelser for sak $sakId")
                    val dto =
                        inTransaction {
                            aktivitetspliktService.hentHendelser(
                                sakId = sakId,
                                behandlingId = behandlingId,
                            )
                        }
                    call.respond(dto)
                }
            }
            post {
                kunSkrivetilgang {
                    logger.info("Oppretter eller oppdaterer hendelser for sakId=$sakId")
                    val hendelse = call.receive<LagreAktivitetspliktHendelse>()
                    val hendelser =
                        inTransaction {
                            aktivitetspliktService.upsertHendelse(hendelse, brukerTokenInfo, sakId = sakId)
                            aktivitetspliktService.hentHendelser(sakId = sakId)
                        }
                    call.respond(hendelser)
                }
            }

            route("/{$HENDELSE_ID_CALL_PARAMETER}") {
                delete {
                    kunSkrivetilgang {
                        logger.info("Sletter hendelse $hendelseId for sakId $sakId")

                        val hendelser =
                            inTransaction {
                                aktivitetspliktService.slettHendelse(hendelseId, sakId = sakId)
                                aktivitetspliktService.hentHendelser(sakId = sakId)
                            }

                        call.respond(hendelser)
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
                    val request = call.receive<OpprettRevurderingForAktivitetspliktDto>()
                    logger.info("Sjekker om sak $sakId trenger en ny revurdering aktivitetsplikt ${request.jobbType.name}")
                    val opprettet =
                        inTransaction {
                            aktivitetspliktService.opprettRevurderingHvisKravIkkeOppfylt(
                                request,
                            )
                        }
                    call.respond(opprettet)
                }
            }
        }

        get("oppgaver-sak") {
            logger.info("Henter oppfølgingsoppgaver for $sakId")
            val oppfoelgingsoppgaver = inTransaction { aktivitetspliktOppgaveService.hentOppfoelgingsoppgaver(sakId) }
            call.respond(oppfoelgingsoppgaver)
        }

        post("/opprett") {
            val request = call.receive<OpprettOppfoelgingsoppgave>()
            inTransaction {
                aktivitetspliktOppgaveService.opprettOppfoelgingsoppgave(request)
            }
            call.respond(HttpStatusCode.OK)
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

    route("/api/aktivitetsplikt/oppgave") {
        route("/{${OPPGAVEID_CALL_PARAMETER}}") {
            get {
                val oppgaveOgVurdering =
                    inTransaction { aktivitetspliktOppgaveService.hentVurderingForOppgave(oppgaveId) }
                call.respond(oppgaveOgVurdering)
            }
            post("/brevdata") {
                val brevdata = call.receive<AktivitetspliktInformasjonBrevdataRequest>()
                val oppdaterBrevdata =
                    inTransaction { aktivitetspliktOppgaveService.lagreBrevdata(oppgaveId, brevdata, brukerTokenInfo) }
                call.respond(oppdaterBrevdata)
            }
            post("/opprettbrev") {
                val brevId =
                    inTransaction {
                        aktivitetspliktOppgaveService.opprettBrevHvisKraveneErOppfyltOgDetIkkeFinnes(
                            oppgaveId = oppgaveId,
                            brukerTokenInfo,
                        )
                    }
                call.respond(BrevIdDto(brevId))
            }
            post("/ferdigstillbrev-og-oppgave") {
                val oppgave =
                    inTransaction { aktivitetspliktOppgaveService.ferdigstillBrevOgOppgave(oppgaveId, brukerTokenInfo) }
                call.respond(oppgave)
            }
            post("/ferdigstill-oppgave") {
                val oppgave =
                    inTransaction {
                        aktivitetspliktOppgaveService.ferdigstillOppgaveUtenBrev(
                            oppgaveId,
                            brukerTokenInfo,
                        )
                    }
                call.respond(oppgave)
            }
        }
    }

    route("/api/sak/{$SAKID_CALL_PARAMETER}/oppgave/{$OPPGAVEID_CALL_PARAMETER}/aktivitetsplikt/vurdering") {
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
                inTransaction { aktivitetspliktService.hentVurderingForBehandlingNy(behandlingId) }
            call.respond(vurdering)
        }

        post("/aktivitetsgrad-unntak") {
            kunSkrivetilgang {
                logger.info("Oppretter aktivitetsgrad og unntak for sakId=$sakId og behandlingId=$behandlingId")
                val aktivitetsgradOgUnntak = call.receive<AktivitetspliktAktivitetsgradOgUnntak>()
                val vurderingForBehandling =
                    inTransaction {
                        aktivitetspliktService.upsertAktivitetsgradOgUnntakForBehandling(
                            aktivitetsgradOgUnntak = aktivitetsgradOgUnntak,
                            behandlingId = behandlingId,
                            sakId = sakId,
                            brukerTokenInfo = brukerTokenInfo,
                        )
                    }
                call.respond(vurderingForBehandling)
            }
        }

        post("/aktivitetsgrad") {
            kunSkrivetilgang {
                logger.info("Redigerer aktivitetsgrad for sakId=$sakId og behandlingId=$behandlingId")
                val aktivitetsgradOgUnntak = call.receive<LagreAktivitetspliktAktivitetsgrad>()
                val vurderingForBehandling =
                    inTransaction {
                        aktivitetspliktService.upsertAktivitetsgradForBehandling(
                            aktivitetsgrad = aktivitetsgradOgUnntak,
                            behandlingId = behandlingId,
                            sakId = sakId,
                            brukerTokenInfo = brukerTokenInfo,
                        )
                    }
                call.respond(vurderingForBehandling)
            }
        }

        delete("/aktivitetsgrad/{$AKTIVITETSGRAD_ID_CALL_PARAMETER}") {
            kunSkrivetilgang {
                logger.info("Sletter aktivitetsgrad med id=$aktivitetsgradId for behandlingId=$behandlingId i sak=$sakId")
                val vurdering =
                    inTransaction {
                        aktivitetspliktService.slettAktivitetsgradForBehandling(
                            behandlingId = behandlingId,
                            aktivitetsgradId = aktivitetsgradId,
                            sakId = sakId,
                        )
                    }
                call.respond(vurdering)
            }
        }

        route("/unntak") {
            post {
                kunSkrivetilgang {
                    logger.info("Oppretter unntak for sakId=$sakId og behandlingId=$behandlingId")
                    val unntak = call.receive<LagreAktivitetspliktUnntak>()
                    val aktivitetspliktVurdering =
                        inTransaction {
                            aktivitetspliktService.upsertUnntakForBehandling(
                                unntak = unntak,
                                behandlingId = behandlingId,
                                sakId = sakId,
                                brukerTokenInfo = brukerTokenInfo,
                            )
                        }
                    call.respond(aktivitetspliktVurdering)
                }
            }
            delete("{$UNNTAK_ID_CALL_PARAMETER}") {
                kunSkrivetilgang {
                    logger.info("Sletter unntak med id=$unntakId for sakId=$sakId og behandlingId=$behandlingId")
                    val vurdering =
                        inTransaction {
                            aktivitetspliktService.slettUnntakForBehandling(
                                behandlingId = behandlingId,
                                sakId = sakId,
                                unntakId = unntakId,
                            )
                        }
                    call.respond(vurdering)
                }
            }
        }
    }
}

data class BrevIdDto(
    val brevId: BrevID? = null,
)
