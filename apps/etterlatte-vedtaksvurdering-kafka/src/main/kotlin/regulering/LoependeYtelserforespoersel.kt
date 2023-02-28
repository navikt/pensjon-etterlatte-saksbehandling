package no.nav.etterlatte.regulering

import no.nav.etterlatte.libs.common.behandling.Omberegningshendelse
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.EventNames.FINN_LOEPENDE_YTELSER
import no.nav.etterlatte.rapidsandrivers.EventNames.OMBEREGNINGSHENDELSE
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.DATO_KEY
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.dato
import rapidsandrivers.sakId
import rapidsandrivers.withFeilhaandtering

internal class LoependeYtelserforespoersel(
    rapidsConnection: RapidsConnection,
    private val vedtak: VedtakService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(LoependeYtelserforespoersel::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(FINN_LOEPENDE_YTELSER)
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(DATO_KEY) }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, FINN_LOEPENDE_YTELSER) {
                val sakId = packet.sakId
                logger.info("Leser reguleringsfoerespoersel for sak $sakId")

                val reguleringsdato = packet.dato
                val respons = vedtak.harLoependeYtelserFra(sakId, reguleringsdato)
                respons.takeIf { it.erLoepende }?.let {
                    packet.eventName = OMBEREGNINGSHENDELSE
                    packet[HENDELSE_DATA_KEY] = Omberegningshendelse(
                        sakId = sakId,
                        fradato = it.dato,
                        aarsak = RevurderingAarsak.GRUNNBELOEPREGULERING,
                        prosesstype = Prosesstype.AUTOMATISK
                    )
                    context.publish(packet.toJson())
                    logger.info("Grunnlopesreguleringmelding ble sendt for sak $sakId. Dato=${respons.dato}")
                } ?: logger.info("Grunnlopesreguleringmelding ble ikke sendt for sak $sakId. Dato=${respons.dato}")
            }
        }
}