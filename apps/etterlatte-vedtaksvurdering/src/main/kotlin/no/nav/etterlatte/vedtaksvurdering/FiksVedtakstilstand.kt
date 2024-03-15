package no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.vedtaksvurdering.UnderkjennVedtakDto
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingService
import org.slf4j.LoggerFactory

class FiksVedtakstilstand(
    val behandlingService: VedtakFiksBehandlingService,
    val vedtakservice: VedtaksvurderingService,
    val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun fiks(bruker: BrukerTokenInfo) {
        if (!featureToggleService.isEnabled(FiksVedtakstilstandFeatureToggle.FiksVedtakstilstand, false)) {
            logger.info("Fiksing av vedtakstilstand er skrudd av. Avbryter uten å gjøre noe.")
            return
        }

        val aktuelleBehandlinger = behandlingService.hentAktuelleBehandlingerForFiksStatus(bruker)
        logger.info("Verifiserer og potensielt retter status for ${aktuelleBehandlinger.size} behandlinger")

        aktuelleBehandlinger.forEach {
            try {
                val brukerTokenInfo = tilBruker(it.ident ?: Fagsaksystem.EY.navn)
                val vedtak = vedtakservice.hentVedtakMedBehandlingId(it.id)
                logger.info("Fikser vedtaksstatus for behandling ${it.id} med status ${it.status} og vedtakstatus ${vedtak?.status}")
                when (it.status) {
                    BehandlingStatus.OPPRETTET,
                    BehandlingStatus.VILKAARSVURDERT,
                    BehandlingStatus.TRYGDETID_OPPDATERT,
                    -> return@forEach
                    BehandlingStatus.BEREGNET,
                    BehandlingStatus.AVKORTET,
                    ->
                        when (vedtak?.status) {
                            VedtakStatus.OPPRETTET -> return@forEach
                            else -> behandlingService.opprettEllerOppdaterVedtak(it.id, brukerTokenInfo)
                        }

                    BehandlingStatus.FATTET_VEDTAK ->
                        when (vedtak?.status) {
                            VedtakStatus.FATTET_VEDTAK -> return@forEach
                            else -> behandlingService.fattVedtak(it.id, brukerTokenInfo)
                        }

                    BehandlingStatus.AVSLAG,
                    BehandlingStatus.ATTESTERT,
                    ->
                        when (vedtak?.status) {
                            VedtakStatus.ATTESTERT -> return@forEach
                            else -> behandlingService.attesterVedtak(it.id, "", brukerTokenInfo)
                        }

                    BehandlingStatus.RETURNERT ->
                        when (vedtak?.status) {
                            VedtakStatus.RETURNERT -> return@forEach
                            else ->
                                behandlingService.underkjennVedtak(
                                    it.id,
                                    brukerTokenInfo,
                                    UnderkjennVedtakDto("", ""),
                                )
                        }

                    BehandlingStatus.TIL_SAMORDNING ->
                        when (vedtak?.status) {
                            VedtakStatus.TIL_SAMORDNING -> return@forEach
                            else -> behandlingService.tilSamordningVedtak(it.id, brukerTokenInfo)
                        }

                    BehandlingStatus.SAMORDNET ->
                        when (vedtak?.status) {
                            VedtakStatus.SAMORDNET -> return@forEach
                            else -> behandlingService.samordnetVedtak(it.id, brukerTokenInfo)
                        }

                    BehandlingStatus.IVERKSATT ->
                        when (vedtak?.status) {
                            VedtakStatus.IVERKSATT -> return@forEach
                            else -> behandlingService.iverksattVedtak(it.id, brukerTokenInfo)
                        }

                    BehandlingStatus.AVBRUTT -> return@forEach
                }
                logger.info("Oppdaterte status for vedtak for behandling ${it.id}")
            } catch (e: Exception) {
                logger.error("Statusoppdatering feila for ${it.id}. Fortsetter med neste behandling", e)
            }
        }
    }

    private fun tilBruker(ident: String) = Systembruker(oid = ident, sub = ident, ident = ident)
}

enum class FiksVedtakstilstandFeatureToggle(private val key: String) : FeatureToggle {
    FiksVedtakstilstand("fiks-vedtakstilstand"),
    ;

    override fun key() = key
}
