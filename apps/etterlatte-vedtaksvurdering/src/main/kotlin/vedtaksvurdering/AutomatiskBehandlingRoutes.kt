package no.nav.etterlatte.vedtaksvurdering

import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sakId
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.token.Fagsaksystem
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient

fun Route.automatiskBehandlingRoutes(
    service: VedtakBehandlingService,
    rapidService: VedtaksvurderingRapidService,
    behandlingKlient: BehandlingKlient,
) {
    route("/api/vedtak") {
        val logger = application.log

        post("/{$SAKID_CALL_PARAMETER}/{$BEHANDLINGID_CALL_PARAMETER}/automatisk") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Håndterer behandling $behandlingId")
                service.opprettEllerOppdaterVedtak(behandlingId, brukerTokenInfo)

                logger.info("Fatter vedtak for behandling $behandlingId")
                service.fattVedtak(behandlingId, brukerTokenInfo).also { rapidService.sendToRapid(it) }

                logger.info("Tildeler attesteringsoppgave til systembruker")
                val oppgaveTilAttestering =
                    behandlingKlient.hentOppgaverForSak(sakId, brukerTokenInfo)
                        .oppgaver
                        .filter { it.referanse == behandlingId.toString() }
                        .filter { it.type == OppgaveType.ATTESTERING }
                        .filterNot { it.erAvsluttet() }
                        .first()
                behandlingKlient.tildelSaksbehandler(oppgaveTilAttestering, brukerTokenInfo)

                logger.info("Attesterer vedtak for behandling $behandlingId")
                val attestert =
                    service.attesterVedtak(
                        behandlingId,
                        "Automatisk attestert av ${Fagsaksystem.EY.systemnavn}",
                        brukerTokenInfo,
                    )

                try {
                    rapidService.sendToRapid(attestert)
                } catch (e: Exception) {
                    logger.error(
                        "Kan ikke sende attestert vedtak på kafka for behandling id: $behandlingId, vedtak: ${attestert.vedtak.vedtakId} " +
                            "Saknr: ${attestert.vedtak.sak.id}. " +
                            "Det betyr at vi ikke sender ut brev for vedtaket eller at en utbetaling går til oppdrag. " +
                            "Denne hendelsen må sendes ut manuelt straks.",
                        e,
                    )
                    throw e
                }

                call.respond(attestert)
            }
        }
    }
}
