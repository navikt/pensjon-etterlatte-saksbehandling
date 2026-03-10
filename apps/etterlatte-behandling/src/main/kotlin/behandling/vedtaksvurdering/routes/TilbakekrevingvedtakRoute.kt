package no.nav.etterlatte.behandling.vedtaksvurdering.routes

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.vedtaksvurdering.LoependeYtelse
import no.nav.etterlatte.behandling.vedtaksvurdering.Vedtak
import no.nav.etterlatte.behandling.vedtaksvurdering.VedtakInnhold
import no.nav.etterlatte.behandling.vedtaksvurdering.VedtakTilbakekrevingService
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingFattEllerAttesterVedtakDto
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingVedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang
import org.slf4j.LoggerFactory

fun Route.tilbakekrevingvedtakRoute(service: VedtakTilbakekrevingService) {
    val logger = LoggerFactory.getLogger("TilbakekrevingsvedtakRoute")
    route("/tilbakekreving/{$BEHANDLINGID_CALL_PARAMETER}") {
        post("/lagre-vedtak") {
            kunSkrivetilgang {
                val dto = call.receive<TilbakekrevingVedtakDto>()
                logger.info("Oppretter vedtak for tilbakekreving=${dto.tilbakekrevingId}")
                call.respond(service.opprettEllerOppdaterVedtak(dto).toDto())
            }
        }
        post("/fatt-vedtak") {
            kunSkrivetilgang {
                val dto = call.receive<TilbakekrevingFattEllerAttesterVedtakDto>()
                logger.info("Fatter vedtak for tilbakekreving=${dto.tilbakekrevingId}")
                call.respond(service.fattVedtak(dto, brukerTokenInfo).toDto())
            }
        }
        post("/attester-vedtak") {
            kunSkrivetilgang {
                val dto = call.receive<TilbakekrevingFattEllerAttesterVedtakDto>()
                logger.info("Attesterer vedtak for tilbakekreving=${dto.tilbakekrevingId}")
                call.respond(service.attesterVedtak(dto, brukerTokenInfo).toDto())
            }
        }
        post("/underkjenn-vedtak") {
            kunSkrivetilgang {
                logger.info("Underkjenner vedtak for tilbakekreving=$behandlingId")
                call.respond(service.underkjennVedtak(behandlingId).toDto())
            }
        }
        post("/tilbakestill-vedtak") {
            kunSkrivetilgang {
                logger.info("Tilbakestiller vedtak fra attestert for tilbakekreving med id=$behandlingId")
                call.respond(service.tilbakeStillAttestert(behandlingId).toDto())
            }
        }
    }
}

private fun Vedtak.toVedtakSammendragDto(): VedtakSammendragDto {
    val dto =
        VedtakSammendragDto(
            id = id.toString(),
            behandlingId = behandlingId,
            vedtakType = type,
            behandlendeSaksbehandler = vedtakFattet?.ansvarligSaksbehandler,
            datoFattet = vedtakFattet?.tidspunkt?.toNorskTid(),
            attesterendeSaksbehandler = attestasjon?.attestant,
            datoAttestert = attestasjon?.tidspunkt?.toNorskTid(),
            virkningstidspunkt = null,
            opphoerFraOgMed = null,
            iverksettelsesTidspunkt = iverksettelsesTidspunkt,
        )
    return when (innhold) {
        is VedtakInnhold.Behandling -> {
            dto.copy(
                virkningstidspunkt = innhold.virkningstidspunkt,
                opphoerFraOgMed = innhold.opphoerFraOgMed,
            )
        }

        else -> {
            dto
        }
    }
}

private fun LoependeYtelse.toDto() =
    LoependeYtelseDTO(
        erLoepende = erLoepende,
        underSamordning = underSamordning,
        dato = dato,
        behandlingId = behandlingId,
        sisteLoependeBehandlingId = sisteLoependeBehandlingId,
    )

data class UnderkjennVedtakDto(
    val kommentar: String,
    val valgtBegrunnelse: String,
)
