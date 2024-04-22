package no.nav.etterlatte.testdata

import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
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

    fun behandle(
        sakId: Long,
        behandling: UUID,
    ) {
        runBlocking {
            logger.info("Starter automatisk behandling av sak $sakId")
            val sak = sakService.hentSak(sakId).body<Sak>()
            logger.info("Henta sak $sakId")
            vilkaarsvurderingService.vilkaarsvurder(behandling)
            logger.info("Vilkårsvurderte behandling $behandling i sak $sakId")
            trygdetidService.beregnTrygdetid(behandling)
            logger.info("Beregna trygdetid for $behandling i sak $sakId")
            beregningService.beregn(behandling)
            logger.info("Beregna behandling $behandling i sak $sakId")
            if (sak.sakType == SakType.OMSTILLINGSSTOENAD) {
                avkortingService.avkort(behandling)
                logger.info("Avkorta behandling $behandling i sak $sakId")
            }
            vedtaksvurderingService.fattVedtak(sakId, behandling)
            brevService.opprettOgDistribuerVedtaksbrev(sakId, behandling)
            vedtaksvurderingService.attesterOgIverksettVedtak(sakId, behandling)
            logger.info("Ferdig iverksatt behandling $behandling i sak $sakId")
        }
    }
}
