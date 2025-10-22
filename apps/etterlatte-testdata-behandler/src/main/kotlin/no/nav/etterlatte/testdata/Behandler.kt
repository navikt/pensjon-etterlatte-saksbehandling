package no.nav.etterlatte.testdata

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.rapidsandrivers.Behandlingssteg
import no.nav.etterlatte.testdata.automatisk.AvkortingService
import no.nav.etterlatte.testdata.automatisk.BehandlingService
import no.nav.etterlatte.testdata.automatisk.BeregningService
import no.nav.etterlatte.testdata.automatisk.BrevService
import no.nav.etterlatte.testdata.automatisk.GrunnlagService
import no.nav.etterlatte.testdata.automatisk.TrygdetidService
import no.nav.etterlatte.testdata.automatisk.VedtaksvurderingService
import no.nav.etterlatte.testdata.automatisk.VilkaarsvurderingService
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.util.UUID

class Behandler(
    private val grunnlagService: GrunnlagService,
    private val behandlingService: BehandlingService,
    private val vilkaarsvurderingService: VilkaarsvurderingService,
    private val trygdetidService: TrygdetidService,
    private val beregningService: BeregningService,
    private val avkortingService: AvkortingService,
    private val brevService: BrevService,
    private val vedtaksvurderingService: VedtaksvurderingService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun behandle(
        sakId: SakId,
        behandling: UUID,
        behandlingssteg: Behandlingssteg,
        packet: JsonMessage,
        context: MessageContext,
        bruker: BrukerTokenInfo,
    ) {
        logger.info("Starter automatisk behandling av sak $sakId og behandling $behandling til steg $behandlingssteg")
        if (behandlingssteg in listOf(Behandlingssteg.KLAR, Behandlingssteg.BEHANDLING_OPPRETTA)) {
            return
        }
        val sak = behandlingService.hentSak(sakId, bruker)
        logger.info("Henta sak $sakId")

        val virkningstidspunkt = utledVirkningstidspunkt(behandling, bruker)

        behandlingService.settKommerBarnetTilGode(behandling, bruker)
        behandlingService.lagreGyldighetsproeving(behandling, bruker)
        behandlingService.lagreUtlandstilknytning(behandling, bruker)
        behandlingService.lagreBoddEllerArbeidetUtlandet(behandling, bruker)
        behandlingService.lagreVirkningstidspunkt(behandling, bruker, virkningstidspunkt)
        behandlingService.tildelSaksbehandler(Fagsaksystem.EY.navn, sakId, bruker)

        logger.info("Tildelt til saksbehandler, klar til vilkårsvurdering")
        vilkaarsvurderingService.vilkaarsvurder(behandling, bruker)
        logger.info("Vilkårsvurderte behandling $behandling i sak $sakId")
        if (behandlingssteg == Behandlingssteg.VILKAARSVURDERT) {
            return
        }
        trygdetidService.beregnTrygdetid(behandling, bruker)
        logger.info("Beregna trygdetid for $behandling i sak $sakId")
        if (behandlingssteg == Behandlingssteg.TRYGDETID_OPPRETTA) {
            return
        }
        logger.info("Lagrer beregningsgrunnlag")
        beregningService.lagreBeregningsgrunnlag(behandling, bruker)
        logger.info("Lagra beregningsgrunnlag, klar til beregning")
        beregningService.beregn(behandling, bruker)
        logger.info("Beregna behandling $behandling i sak $sakId")
        if (behandlingssteg == Behandlingssteg.BEREGNA) {
            return
        }
        logger.info("Ferdig beregna i $behandling")

        if (sak.sakType == SakType.OMSTILLINGSSTOENAD) {
            logger.info("Avkorter $behandling")
            avkortingService.avkort(behandling, virkningstidspunkt, bruker)
            logger.info("Avkorta behandling $behandling i sak $sakId")
        }
        if (behandlingssteg == Behandlingssteg.AVKORTA) {
            return
        }
        logger.info("Klar til å lagre brevutfall for $behandling")
        behandlingService.lagreBrevutfall(behandling, sak.sakType, bruker)
        logger.info("Ferdig med å lagre brevutfall for behandling $behandling. Klar til å fatte vedtak")
        val fattaVedtak = vedtaksvurderingService.fattVedtak(sakId, behandling, bruker)
        RapidUtsender.sendUt(fattaVedtak, packet, context)
        if (behandlingssteg == Behandlingssteg.VEDTAK_FATTA) {
            return
        }

        logger.info("Fatta vedtak for behandling $behandling. Klar til å lage og distribuere vedtaksbrev")
        brevService.opprettOgDistribuerVedtaksbrev(sakId, behandling, bruker)
        val attestertVedtak =
            vedtaksvurderingService.attesterOgIverksettVedtak(
                sakId,
                behandling,
                bruker,
            )
        logger.info("Ferdig attestert behandling $behandling i sak $sakId")
        RapidUtsender.sendUt(attestertVedtak, packet, context)
    }

    private fun utledVirkningstidspunkt(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ): YearMonth {
        val doedsdato =
            runBlocking {
                grunnlagService
                    .hentGrunnlagForBehandling(behandlingId, bruker)
                    .hentAvdoede()
                    .first()
                    .hentDoedsdato()
                    ?.verdi!!
            }
        val maanedEtterDoedsfall = doedsdato.plusMonths(1)
        return YearMonth.of(maanedEtterDoedsfall.year, maanedEtterDoedsfall.month)
    }
}
