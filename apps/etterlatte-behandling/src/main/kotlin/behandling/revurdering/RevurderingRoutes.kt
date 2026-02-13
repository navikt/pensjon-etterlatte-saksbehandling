package no.nav.etterlatte.behandling.revurdering

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.etteroppgjoer.ETTEROPPGJOER_AAR
import no.nav.etterlatte.behandling.etteroppgjoer.revurdering.EtteroppgjoerRevurderingService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.inntektsjustering.AarligInntektsjusteringJobbService
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.revurdering.AutomatiskRevurderingRequest
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.medBody
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.tilgangsstyring.kunSaksbehandlerMedSkrivetilgang
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang
import org.slf4j.LoggerFactory
import java.util.UUID

internal fun Route.revurderingRoutes(
    revurderingService: RevurderingService,
    manuellRevurderingService: ManuellRevurderingService,
    omgjoeringKlageRevurderingService: OmgjoeringKlageRevurderingService,
    automatiskRevurderingService: AutomatiskRevurderingService,
    aarligInntektsjusteringJobbService: AarligInntektsjusteringJobbService,
    etteroppgjoerRevurderingService: EtteroppgjoerRevurderingService,
) {
    val logger = LoggerFactory.getLogger("RevurderingRoute")

    route("/api/revurdering") {
        route("{$BEHANDLINGID_CALL_PARAMETER}") {
            route("revurderinginfo") {
                post {
                    kunSkrivetilgang {
                        logger.info("Lagrer revurderinginfo på behandling $behandlingId")
                        medBody<RevurderingInfoDto> {
                            inTransaction {
                                revurderingService.lagreRevurderingInfo(
                                    behandlingId,
                                    RevurderingInfoMedBegrunnelse(it.info, it.begrunnelse),
                                    brukerTokenInfo.ident(),
                                )
                            }
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }
                }
            }
        }

        route("/{$SAKID_CALL_PARAMETER}") {
            post {
                kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
                    logger.info("Oppretter ny revurdering på sak $sakId")
                    medBody<OpprettRevurderingRequest> { opprettRevurderingRequest ->
                        // TODO: er feil i denne flyten da vi ikke kan gjøre tilgangssjekk for grunnlag da behandlingen ikke finnes enda
                        val revurdering =
                            inTransaction {
                                manuellRevurderingService.opprettManuellRevurderingWrapper(
                                    sakId = sakId,
                                    aarsak = opprettRevurderingRequest.aarsak,
                                    paaGrunnAvHendelseId = opprettRevurderingRequest.paaGrunnAvHendelseId,
                                    paaGrunnAvOppgaveId = opprettRevurderingRequest.paaGrunnAvOppgaveId,
                                    begrunnelse = opprettRevurderingRequest.begrunnelse,
                                    fritekstAarsak = opprettRevurderingRequest.fritekstAarsak,
                                    saksbehandler = saksbehandler,
                                )
                            }
                        call.respond(revurdering.id)
                    }
                }
            }

            post("/etteroppgjoer") {
                kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
                    logger.info("Oppretter ny revurdering på sak $sakId")

                    medBody<OpprettEtteroppgjoerRevurderingRequest> {
                        val revurdering =
                            etteroppgjoerRevurderingService.opprettEtteroppgjoerRevurdering(
                                sakId,
                                ETTEROPPGJOER_AAR,
                                it.opprinnelse,
                                saksbehandler,
                            )

                        call.respond(revurdering.id)
                    }
                }
            }

            post("manuell-inntektsjustering") {
                kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
                    logger.info("Oppretter ny revurdering for årlig manuell inntektsjustering på sak $sakId")
                    medBody<OpprettManuellInntektsjustering> {
                        val revurdering =
                            inTransaction {
                                aarligInntektsjusteringJobbService.opprettRevurderingForAarligInntektsjustering(
                                    sakId,
                                    it.oppgaveId,
                                    saksbehandler,
                                )
                            }
                        call.respond(revurdering.id)
                    }
                }
            }

            post("omgjoering-klage") {
                kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
                    medBody<OpprettOmgjoeringKlageRequest> {
                        val revurdering =
                            inTransaction {
                                omgjoeringKlageRevurderingService.opprettOmgjoeringKlage(
                                    sakId,
                                    it.oppgaveIdOmgjoering,
                                    saksbehandler,
                                )
                            }
                        call.respond(revurdering)
                    }
                }
            }

            get("/{revurderingsaarsak}") {
                val revurderingsaarsak =
                    call.parameters["revurderingsaarsak"]?.let { Revurderingaarsak.valueOf(it) }
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Ugyldig revurderingsårsak")
                call.respond(
                    inTransaction {
                        revurderingService.hentRevurderingsinfoForSakMedAarsak(
                            sakId,
                            revurderingsaarsak,
                        )
                    },
                )
            }
        }
    }

    route("/api/stoettederevurderinger") {
        get("/{saktype}") {
            val sakType =
                call.parameters["saktype"]?.let { SakType.valueOf(it) }
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Ugyldig saktype")

            val stoettedeRevurderinger = hentRevurderingaarsaker(sakType)

            call.respond(stoettedeRevurderinger)
        }
    }

    route("/automatisk-revurdering") {
        post {
            kunSystembruker { systembruker ->
                val request = call.receive<AutomatiskRevurderingRequest>()
                val response = automatiskRevurderingService.oppprettRevurderingOgOppfoelging(request, systembruker)
                call.respond(response)
            }
        }
    }
}

private fun hentRevurderingaarsaker(sakType: SakType) =
    Revurderingaarsak.entries
        .filter { it.erStoettaRevurdering(sakType) }

data class OpprettOmgjoeringKlageRequest(
    val oppgaveIdOmgjoering: UUID,
)

data class OpprettRevurderingRequest(
    val aarsak: Revurderingaarsak,
    val paaGrunnAvHendelseId: String? = null,
    val paaGrunnAvOppgaveId: String? = null,
    val begrunnelse: String? = null,
    val fritekstAarsak: String? = null,
)

data class OpprettManuellInntektsjustering(
    val oppgaveId: UUID,
)

data class RevurderingInfoDto(
    val begrunnelse: String?,
    val info: RevurderingInfo,
)

data class OpprettEtteroppgjoerRevurderingRequest(
    val opprinnelse: BehandlingOpprinnelse,
    val inntektsaar: Int,
)
