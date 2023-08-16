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
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandlingsId
import no.nav.etterlatte.libs.common.hentNavidentFraToken
import no.nav.etterlatte.libs.common.kunSaksbehandler
import no.nav.etterlatte.libs.common.medBody
import no.nav.etterlatte.libs.common.sakId

internal fun Route.revurderingRoutes(
    revurderingService: RevurderingService
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
                kunSaksbehandler { saksbehandler ->
                    logger.info("Oppretter ny revurdering på sak $sakId")
                    medBody<OpprettRevurderingRequest> { opprettRevurderingRequest ->
                        if (!opprettRevurderingRequest.aarsak.kanBrukesIMiljo()) {
                            return@post call.respond(
                                HttpStatusCode.BadRequest,
                                "Feil revurderingsårsak ${opprettRevurderingRequest.aarsak}, foreløpig ikke støttet"
                            )
                        }

                        val revurdering = revurderingService.opprettManuellRevurderingWrapper(
                            sakId,
                            opprettRevurderingRequest.aarsak,
                            opprettRevurderingRequest.paaGrunnAvHendelseId,
                            opprettRevurderingRequest.begrunnelse,
                            opprettRevurderingRequest.fritekstAarsak,
                            saksbehandler.ident
                        )

                        when (revurdering) {
                            null -> call.respond(HttpStatusCode.NotFound)
                            else -> call.respond(revurdering.id)
                        }
                    }
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
    val begrunnelse: String? = null,
    val fritekstAarsak: String? = null
)

data class RevurderingInfoDto(val info: RevurderingInfo)