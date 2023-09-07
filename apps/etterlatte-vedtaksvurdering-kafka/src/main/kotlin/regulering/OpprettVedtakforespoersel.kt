package no.nav.etterlatte.regulering

import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.EventNames.OPPRETT_VEDTAK
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.DATO_KEY
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.migrering.ListenerMedLogging
import rapidsandrivers.sakId
import rapidsandrivers.withFeilhaandtering

internal class OpprettVedtakforespoersel(
    rapidsConnection: RapidsConnection,
    private val vedtak: VedtakService
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(OpprettVedtakforespoersel::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(OPPRETT_VEDTAK)
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(DATO_KEY) }
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            correlationId()
        }.register(this)
    }

    override fun haandterPakke(packet: JsonMessage, context: MessageContext) {
        val sakId = packet.sakId
        logger.info("Leser opprett-vedtak forespoersel for sak $sakId")
        val behandlingId = packet.behandlingId

        withFeilhaandtering(packet, context, OPPRETT_VEDTAK) {
            val respons = vedtak.opprettVedtakFattOgAttester(packet.sakId, behandlingId)
            logger.info("Opprettet vedtak ${respons.vedtakId} for sak: $sakId og behandling: $behandlingId")
        }
    }
}