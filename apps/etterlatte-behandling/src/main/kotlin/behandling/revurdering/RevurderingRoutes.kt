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
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandlingId
import no.nav.etterlatte.libs.common.hentNavidentFraToken
import no.nav.etterlatte.libs.common.medBody
import no.nav.etterlatte.libs.common.sakId
import no.nav.etterlatte.tilgangsstyring.kunSaksbehandlerMedSkrivetilgang
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang

enum class RevurderingRoutesFeatureToggle(private val key: String) : FeatureToggle {
    VisRevurderingsaarsakOpphoerUtenBrev("pensjon-etterlatte.vis-opphoer-uten-brev"),
    ;

    override fun key() = key
}

internal fun Route.revurderingRoutes(
    revurderingService: RevurderingService,
    featureToggleService: FeatureToggleService,
) {
    val logger = application.log

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
                                    sakId,
                                    opprettRevurderingRequest.aarsak,
                                    opprettRevurderingRequest.paaGrunnAvHendelseId,
                                    opprettRevurderingRequest.begrunnelse,
                                    opprettRevurderingRequest.fritekstAarsak,
                                    saksbehandler,
                                )
                            }

                        when (revurdering) {
                            null -> call.respond(HttpStatusCode.NotFound)
                            else -> call.respond(revurdering.id)
                        }
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

    route("/api/stoettederevurderinger/{saktype}") {
        get {
            val sakType =
                call.parameters["saktype"]?.let { SakType.valueOf(it) }
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Ugyldig saktype")

            val stoettedeRevurderinger =
                Revurderingaarsak.entries
                    .filter { it.erStoettaRevurdering(sakType) }
                    .filter {
                        if (it == Revurderingaarsak.OPPHOER_UTEN_BREV) {
                            featureToggleService.isEnabled(
                                RevurderingRoutesFeatureToggle.VisRevurderingsaarsakOpphoerUtenBrev,
                                false,
                            )
                        } else {
                            true
                        }
                    }

            call.respond(stoettedeRevurderinger)
        }
    }
}

data class OpprettRevurderingRequest(
    val aarsak: Revurderingaarsak,
    val paaGrunnAvHendelseId: String? = null,
    val begrunnelse: String? = null,
    val fritekstAarsak: String? = null,
)

data class RevurderingInfoDto(val begrunnelse: String?, val info: RevurderingInfo)
