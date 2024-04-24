package no.nav.etterlatte.testdata

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.rapidsandrivers.Behandlingssteg
import no.nav.etterlatte.testdata.automatisk.AvkortingService
import no.nav.etterlatte.testdata.automatisk.BeregningService
import no.nav.etterlatte.testdata.automatisk.BrevService
import no.nav.etterlatte.testdata.automatisk.SakService
import no.nav.etterlatte.testdata.automatisk.TrygdetidService
import no.nav.etterlatte.testdata.automatisk.VedtaksvurderingService
import no.nav.etterlatte.testdata.automatisk.VilkaarsvurderingService
import org.slf4j.LoggerFactory
import java.util.UUID

class Behandler(
    private val sakService: SakService,
    private val vilkaarsvurderingService: VilkaarsvurderingService,
    private val trygdetidService: TrygdetidService,
    private val beregningService: BeregningService,
    private val avkortingService: AvkortingService,
    private val brevService: BrevService,
    private val vedtaksvurderingService: VedtaksvurderingService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun behandle(
        sakId: Long,
        behandling: UUID,
        behandlingssteg: Behandlingssteg,
    ) {
        logger.info("Starter automatisk behandling av sak $sakId og behandling $behandling til steg $behandlingssteg")
        if (behandlingssteg in listOf(Behandlingssteg.KLAR, Behandlingssteg.BEHANDLING_OPPRETTA)) {
            return
        }
        val sak = sakService.hentSak(sakId)
        logger.info("Henta sak $sakId")

        sakService.settKommerBarnetTilGode(behandling)
        sakService.lagreGyldighetsproeving(behandling)
        sakService.lagreVirkningstidspunkt(behandling)
        sakService.tildelSaksbehandler(Fagsaksystem.EY.navn, sakId)

        vilkaarsvurderingService.vilkaarsvurder(behandling)
        logger.info("Vilkårsvurderte behandling $behandling i sak $sakId")
        if (behandlingssteg == Behandlingssteg.VILKAARSVURDERT) {
            return
        }
        trygdetidService.beregnTrygdetid(behandling)
        logger.info("Beregna trygdetid for $behandling i sak $sakId")
        if (behandlingssteg == Behandlingssteg.TRYGDETID_OPPRETTA) {
            return
        }
        beregningService.beregn(behandling)
        logger.info("Beregna behandling $behandling i sak $sakId")
        if (behandlingssteg == Behandlingssteg.BEREGNA) {
            return
        }

        if (sak.sakType == SakType.OMSTILLINGSSTOENAD) {
            avkortingService.avkort(behandling)
            logger.info("Avkorta behandling $behandling i sak $sakId")
        }
        if (behandlingssteg == Behandlingssteg.AVKORTA) {
            return
        }
        vedtaksvurderingService.fattVedtak(sakId, behandling)
        if (behandlingssteg == Behandlingssteg.VEDTAK_FATTA) {
            return
        }

        brevService.opprettOgDistribuerVedtaksbrev(sakId, behandling)
        vedtaksvurderingService.attesterOgIverksettVedtak(sakId, behandling)
        logger.info("Ferdig iverksatt behandling $behandling i sak $sakId")
    }
}
