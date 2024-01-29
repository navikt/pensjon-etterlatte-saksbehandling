package no.nav.etterlatte.regulering

import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.rapidsandrivers.ReguleringHendelseType
import no.nav.etterlatte.vedtaksvurdering.RapidUtsender
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.DATO_KEY
import rapidsandrivers.ListenerMedLogging
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.sakId
import rapidsandrivers.withFeilhaandtering

internal class OpprettVedtakforespoerselRiver(
    rapidsConnection: RapidsConnection,
    private val vedtak: VedtakService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(OpprettVedtakforespoerselRiver::class.java)

    init {
        initialiserRiver(rapidsConnection, ReguleringHendelseType.OPPRETT_VEDTAK) {
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(DATO_KEY) }
            validate { it.requireKey(BEHANDLING_ID_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val sakId = packet.sakId
        logger.info("Leser opprett-vedtak forespoersel for sak $sakId")
        val behandlingId = packet.behandlingId

        withFeilhaandtering(packet, context, ReguleringHendelseType.OPPRETT_VEDTAK.lagEventnameForType()) {
            val respons = vedtak.opprettVedtakFattOgAttester(packet.sakId, behandlingId)
            logger.info("Opprettet vedtak ${respons.vedtak.id} for sak: $sakId og behandling: $behandlingId")
            RapidUtsender.sendUt(respons, packet, context)
        }
    }
}
