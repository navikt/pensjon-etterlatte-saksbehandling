package no.nav.etterlatte.behandling.revurdering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.hentNavidentFraToken
import no.nav.etterlatte.libs.ktor.route.medBody
import no.nav.etterlatte.libs.ktor.route.routeLogger
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.tilgangsstyring.kunSaksbehandlerMedSkrivetilgang
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang
import java.util.UUID

internal fun Route.revurderingRoutes(revurderingService: RevurderingService) {
    val logger = routeLogger

    route("/api/revurdering") {
        route("{$BEHANDLINGID_CALL_PARAMETER}") {
            route("revurderinginfo") {
                post {
                    kunSkrivetilgang {
                        hentNavidentFraToken { navIdent ->
                            logger.info("Lagrer revurderinginfo på behandling $behandlingId")
                            medBody<RevurderingInfoDto> {
                                inTransaction {
                                    revurderingService.lagreRevurderingInfo(
                                        behandlingId,
                                        RevurderingInfoMedBegrunnelse(it.info, it.begrunnelse),
                                        navIdent,
                                    )
                                }
                                call.respond(HttpStatusCode.NoContent)
                            }
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

                        val revurdering =
                            inTransaction {
                                revurderingService.opprettManuellRevurderingWrapper(
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

            post("omgjoering-klage") {
                kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
                    medBody<OpprettOmgjoeringKlageRequest> {
                        val revurdering =
                            inTransaction {
                                revurderingService.opprettOmgjoeringKlage(
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
                call.respond(inTransaction { revurderingService.hentRevurderingsinfoForSakMedAarsak(sakId, revurderingsaarsak) })
            }
        }
    }

    route("/api/stoettederevurderinger") {
        get {
            val stoettedeRevurderinger =
                SakType.entries.associateWith { hentRevurderingaarsaker(it) }
            call.respond(stoettedeRevurderinger)
        }

        get("/{saktype}") {
            val sakType =
                call.parameters["saktype"]?.let { SakType.valueOf(it) }
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Ugyldig saktype")

            val stoettedeRevurderinger = hentRevurderingaarsaker(sakType)

            call.respond(stoettedeRevurderinger)
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

data class RevurderingInfoDto(val begrunnelse: String?, val info: RevurderingInfo)
