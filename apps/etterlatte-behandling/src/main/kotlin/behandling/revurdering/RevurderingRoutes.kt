package no.nav.etterlatte.behandling.revurdering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandlingsId
import no.nav.etterlatte.libs.common.hentNavidentFraToken
import no.nav.etterlatte.libs.common.sakId
import java.util.*

internal fun Route.revurderingRoutes(
    revurderingService: RevurderingService,
    generellBehandlingService: GenerellBehandlingService
) {
    val logger = application.log

    route("/api/revurdering") {
        route("{$BEHANDLINGSID_CALL_PARAMETER}") {
            route("revurderinginfo") {
                post {
                    hentNavidentFraToken { navIdent ->
                        logger.info("Lagrer revurderinginfo på behandling $behandlingsId")
                        val dto = try {
                            call.receive<RevurderingInfoDto>()
                        } catch (e: Exception) {
                            return@post call.respond(HttpStatusCode.BadRequest)
                        }
                        val fikkLagret = revurderingService.lagreRevurderingInfo(behandlingsId, dto.info, navIdent)
                        if (fikkLagret) {
                            call.respond(HttpStatusCode.NoContent)
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    }
                }
            }
        }

        route("{$SAKID_CALL_PARAMETER}") {
            post {
                logger.info("Oppretter ny revurdering på sak $sakId")
                val body = try {
                    call.receive<OpprettRevurderingRequest>()
                } catch (e: Exception) {
                    logger.error("Feil skjedde under lesing av payloaden.", e)
                    call.respond(HttpStatusCode.BadRequest, "Feil under deserialiseringen av objektet")
                    return@post
                }
                if (!body.aarsak.kanBrukesIMiljo()) {
                    call.respond(HttpStatusCode.BadRequest, "Feil revurderingsårsak, foreløpig ikke støttet")
                    return@post
                }
                generellBehandlingService.hentSisteIverksatte(sakId)?.let { forrigeIverksatteBehandling ->
                    val sakType = forrigeIverksatteBehandling.sak.sakType
                    if (!body.aarsak.gyldigForSakType(sakType)) {
                        call.respond(HttpStatusCode.BadRequest, "${body.aarsak} er ikke støttet for $sakType")
                        return@post
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
                        paaGrunnAvHendelse = paaGrunnAvHendelseId
                    )

                    when (revurdering) {
                        null -> call.respond(HttpStatusCode.NotFound)
                        else -> call.respond(revurdering.id)
                    }
                } ?: call.respond(HttpStatusCode.BadRequest, "Kan ikke revurdere en sak uten iverksatt behandling")
            }
        }
    }

    route("/api/stoettederevurderinger/{saktype}") {
        get {
            val sakType = call.parameters["saktype"]?.let { runCatching { SakType.valueOf(it) }.getOrNull() }
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Ugyldig saktype")

            val stoettedeRevurderinger = RevurderingAarsak.values()
                .filter { it.kanBrukesIMiljo() && it.gyldigForSakType(sakType) }
                .filter { it.name !== RevurderingAarsak.NY_SOEKNAD.toString() }
            call.respond(stoettedeRevurderinger)
        }
    }
}

data class OpprettRevurderingRequest(val aarsak: RevurderingAarsak, val paaGrunnAvHendelseId: String? = null)

data class RevurderingInfoDto(val info: RevurderingInfo)