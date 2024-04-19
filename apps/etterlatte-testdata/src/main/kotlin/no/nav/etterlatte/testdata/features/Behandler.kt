package no.nav.etterlatte.no.nav.etterlatte.testdata.features

import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.no.nav.etterlatte.testdata.automatisk.AvkortingService
import no.nav.etterlatte.no.nav.etterlatte.testdata.automatisk.BeregningService
import no.nav.etterlatte.no.nav.etterlatte.testdata.automatisk.BrevService
import no.nav.etterlatte.no.nav.etterlatte.testdata.automatisk.SakService
import no.nav.etterlatte.no.nav.etterlatte.testdata.automatisk.TrygdetidService
import no.nav.etterlatte.no.nav.etterlatte.testdata.automatisk.VedtaksvurderingService
import no.nav.etterlatte.no.nav.etterlatte.testdata.automatisk.VilkaarsvurderingService
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.Behandlingssteg
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.behandlingId
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

class Behandler(
    rapidsConnection: RapidsConnection,
    private val sakService: SakService,
    private val vilkaarsvurderingService: VilkaarsvurderingService,
    private val trygdetidService: TrygdetidService,
    private val beregningService: BeregningService,
    private val avkortingService: AvkortingService,
    private val brevService: BrevService,
    private val vedtaksvurderingService: VedtaksvurderingService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, EventNames.NY_OPPLYSNING) {
            validate { it.requireKey(Behandlingssteg.KEY) }
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(BEHANDLING_ID_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val sakId = packet.sakId
        val behandling = packet.behandlingId

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
            vedtaksvurderingService.fattAttesterOgIverksettVedtak(behandling)
            brevService.opprettOgDistribuerVedtaksbrev(behandling)
            logger.info("Ferdig iverksatt behandling $behandling i sak $sakId")
        }
    }

    override fun kontekst() = Kontekst.TEST
}
