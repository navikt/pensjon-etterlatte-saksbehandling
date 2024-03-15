package no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.vedtaksvurdering.UnderkjennVedtakDto
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingService
import org.slf4j.LoggerFactory

class FiksVedtakstilstand(val behandlingService: VedtakFiksBehandlingService, val vedtakservice: VedtaksvurderingService) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun fiks(bruker: BrukerTokenInfo) {
        val aktuelleBehandlinger = behandlingService.hentAktuelleBehandlingerForFiksStatus(bruker)
        logger.info("Verifiserer og potensielt retter status for ${aktuelleBehandlinger.size} behandlinger")

        aktuelleBehandlinger.forEach {
            val brukerTokenInfo = tilBruker(it.ident ?: Fagsaksystem.EY.navn)
            val vedtak = vedtakservice.hentVedtakMedBehandlingId(it.id)
            logger.info("Fikser vedtaksstatus for behandling ${it.id} med status ${it.status} og vedtakstatus ${vedtak?.status}")
            when (it.status) {
                BehandlingStatus.OPPRETTET,
                BehandlingStatus.VILKAARSVURDERT,
                BehandlingStatus.TRYGDETID_OPPDATERT,
                BehandlingStatus.BEREGNET,
                BehandlingStatus.AVKORTET,
                BehandlingStatus.AVSLAG,
                ->
                    when (vedtak?.status) {
                        null -> return
                        VedtakStatus.OPPRETTET -> return
                        else -> behandlingService.opprettEllerOppdaterVedtak(it.id, brukerTokenInfo)
                    }
                BehandlingStatus.FATTET_VEDTAK ->
                    when (vedtak?.status) {
                        VedtakStatus.FATTET_VEDTAK -> return
                        else -> behandlingService.fattVedtak(it.id, brukerTokenInfo)
                    }

                BehandlingStatus.ATTESTERT ->
                    when (vedtak?.status) {
                        VedtakStatus.ATTESTERT -> return
                        else -> behandlingService.attesterVedtak(it.id, "", brukerTokenInfo)
                    }
                BehandlingStatus.RETURNERT ->
                    when (vedtak?.status) {
                        VedtakStatus.RETURNERT -> return
                        else -> behandlingService.underkjennVedtak(it.id, brukerTokenInfo, UnderkjennVedtakDto("", ""))
                    }
                BehandlingStatus.TIL_SAMORDNING ->
                    when (vedtak?.status) {
                        VedtakStatus.TIL_SAMORDNING -> return
                        else -> behandlingService.tilSamordningVedtak(it.id, brukerTokenInfo)
                    }
                BehandlingStatus.SAMORDNET ->
                    when (vedtak?.status) {
                        VedtakStatus.SAMORDNET -> return
                        else -> behandlingService.samordnetVedtak(it.id, brukerTokenInfo)
                    }
                BehandlingStatus.IVERKSATT ->
                    when (vedtak?.status) {
                        VedtakStatus.IVERKSATT -> return
                        else -> behandlingService.iverksattVedtak(it.id, brukerTokenInfo)
                    }
                BehandlingStatus.AVBRUTT -> return
            }
        }
    }

    private fun tilBruker(ident: String) = Systembruker(oid = ident, sub = ident, ident = ident)
}
