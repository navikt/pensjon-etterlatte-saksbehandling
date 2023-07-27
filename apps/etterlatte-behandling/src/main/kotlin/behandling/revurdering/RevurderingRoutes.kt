package no.nav.etterlatte.behandling.revurdering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandlingsId
import no.nav.etterlatte.libs.common.hentNavidentFraToken
import no.nav.etterlatte.libs.common.medBody
import no.nav.etterlatte.libs.common.sakId
import java.util.*

internal fun Route.revurderingRoutes(
    revurderingService: RevurderingService,
    behandlingService: BehandlingService
) {
    val logger = application.log

    route("/api/revurdering") {
        route("{$BEHANDLINGSID_CALL_PARAMETER}") {
            route("revurderinginfo") {
                post {
                    hentNavidentFraToken { navIdent ->
                        logger.info("Lagrer revurderinginfo på behandling $behandlingsId")
                        medBody<RevurderingInfoDto> {
                            val fikkLagret = revurderingService.lagreRevurderingInfo(behandlingsId, it.info, navIdent)
                            if (fikkLagret) {
                                call.respond(HttpStatusCode.NoContent)
                            } else {
                                call.respond(HttpStatusCode.Forbidden)
                            }
                        }
                    }
                }
            }
        }

        route("{$SAKID_CALL_PARAMETER}") {
            post {
                logger.info("Oppretter ny revurdering på sak $sakId")
                medBody<OpprettRevurderingRequest> { body ->
                    if (!body.aarsak.kanBrukesIMiljo()) {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            "Feil revurderingsårsak ${body.aarsak}, foreløpig ikke støttet"
                        )
                    }
                    behandlingService.hentSisteIverksatte(sakId)?.let { forrigeIverksatteBehandling ->
                        val sakType = forrigeIverksatteBehandling.sak.sakType
                        if (!body.aarsak.gyldigForSakType(sakType)) {
                            return@post call.respond(
                                HttpStatusCode.BadRequest,
                                "${body.aarsak} er ikke støttet for $sakType"
                            )
                        }

                        val paaGrunnAvHendelseId = try {
                            body.paaGrunnAvHendelseId?.let { UUID.fromString(it) }
                        } catch (e: Exception) {
                            return@post call.respond(
                                HttpStatusCode.BadRequest,
                                "${body.paaGrunnAvHendelseId} er ikke en gyldig UUID"
                            )
                        }

                        val revurdering = revurderingService.opprettManuellRevurdering(
                            sakId = forrigeIverksatteBehandling.sak.id,
                            forrigeBehandling = forrigeIverksatteBehandling,
                            revurderingAarsak = body.aarsak,
                            kilde = Vedtaksloesning.GJENNY,
                            paaGrunnAvHendelse = paaGrunnAvHendelseId,
                            begrunnelse = body.begrunnelse
                        )

                        when (revurdering) {
                            null -> call.respond(HttpStatusCode.NotFound)
                            else -> call.respond(revurdering.id)
                        }
                    } ?: call.respond(HttpStatusCode.BadRequest, "Kan ikke revurdere en sak uten iverksatt behandling")
                }
            }
        }
    }

    route("/api/stoettederevurderinger/{saktype}") {
        get {
            val sakType = call.parameters["saktype"]?.let { runCatching { SakType.valueOf(it) }.getOrNull() }
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Ugyldig saktype")

            val stoettedeRevurderinger = RevurderingAarsak.values().filter { it.erStoettaRevurdering(sakType) }
            call.respond(stoettedeRevurderinger)
        }
    }
}

data class OpprettRevurderingRequest(
    val aarsak: RevurderingAarsak,
    val paaGrunnAvHendelseId: String? = null,
    val begrunnelse: String? = null
)

data class RevurderingInfoDto(val info: RevurderingInfo)