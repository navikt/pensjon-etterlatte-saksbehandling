package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringKjoringVariant
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Fagsaksystem
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import org.slf4j.LoggerFactory
import java.util.UUID

class AutomatiskBehandlingService(
    val service: VedtakBehandlingService,
    val rapidService: VedtaksvurderingRapidService,
    val behandlingKlient: BehandlingKlient,
) {
    private val logger = LoggerFactory.getLogger(AutomatiskBehandlingService::class.java)

    suspend fun vedtakStegvis(
        behandlingId: UUID,
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
        kjoringVariant: MigreringKjoringVariant,
    ): VedtakOgRapid =
        when (kjoringVariant) {
            MigreringKjoringVariant.FULL_KJORING -> {
                opprettOgFattVedtak(behandlingId, sakId, brukerTokenInfo)
                attesterVedtak(behandlingId, brukerTokenInfo)
            }

            MigreringKjoringVariant.MED_PAUSE -> opprettOgFattVedtak(behandlingId, sakId, brukerTokenInfo)
            MigreringKjoringVariant.FORTSETT_ETTER_PAUSE -> attesterVedtak(behandlingId, brukerTokenInfo)
        }

    private suspend fun opprettOgFattVedtak(
        behandlingId: UUID,
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakOgRapid {
        logger.info("Håndterer behandling $behandlingId")
        service.opprettEllerOppdaterVedtak(behandlingId, brukerTokenInfo)
        logger.info("Fatter vedtak for behandling $behandlingId")
        val vedtakOgRapid = service.fattVedtak(behandlingId, brukerTokenInfo).also { rapidService.sendToRapid(it) }

        logger.info("Tildeler attesteringsoppgave til systembruker")
        val oppgaveTilAttestering =
            behandlingKlient.hentOppgaverForSak(sakId, brukerTokenInfo)
                .oppgaver
                .filter { it.referanse == behandlingId.toString() }
                .filter { it.type == OppgaveType.ATTESTERING }
                .filterNot { it.erAvsluttet() }
                .first()
        behandlingKlient.tildelSaksbehandler(oppgaveTilAttestering, brukerTokenInfo)
        return vedtakOgRapid
    }

    private suspend fun attesterVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakOgRapid {
        logger.info("Attesterer vedtak for behandling $behandlingId")
        return service.attesterVedtak(
            behandlingId,
            "Automatisk attestert av ${Fagsaksystem.EY.systemnavn}",
            brukerTokenInfo,
        ).also { rapidService.sendToRapid(it) }
    }
}
