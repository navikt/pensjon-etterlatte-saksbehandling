package no.nav.etterlatte.regulering

import no.nav.etterlatte.libs.common.behandling.Omberegningshendelse
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.rapidsandrivers.sakId
import no.nav.etterlatte.libs.common.rapidsandrivers.sakIdKey
import no.nav.etterlatte.rapidsandrivers.EventNames.FINN_LOEPENDE_YTELSER
import no.nav.etterlatte.rapidsandrivers.EventNames.OMBEREGNINGSHENDELSE
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import org.slf4j.LoggerFactory

internal class LoependeYtelserforespoersel(
    rapidsConnection: RapidsConnection,
    private val vedtak: VedtakService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(LoependeYtelserforespoersel::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(FINN_LOEPENDE_YTELSER)
            validate { it.requireKey(sakIdKey) }
            validate { it.requireKey("dato") }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            val sakId = packet.sakId
            logger.info("Leser reguleringsfoerespoersel for sak $sakId")

            val reguleringsdato = packet["dato"].asLocalDate()
            val respons = vedtak.harLoependeYtelserFra(sakId, reguleringsdato)
            respons.takeIf { it.erLoepende }?.let {
                packet.eventName = OMBEREGNINGSHENDELSE
                packet["hendelse_data"] = Omberegningshendelse(
                    sakId = sakId,
                    fradato = it.dato,
                    aarsak = RevurderingAarsak.GRUNNBELOEPREGULERING
                )
                context.publish(packet.toJson())
                logger.info("Grunnlopesreguleringmelding ble sendt for sak $sakId. Dato=${respons.dato}")
            } ?: logger.info("Grunnlopesreguleringmelding ble ikke sendt for sak $sakId. Dato=${respons.dato}")
        }
}