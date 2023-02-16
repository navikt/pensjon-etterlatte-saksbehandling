package no.nav.etterlatte.regulering

import no.nav.etterlatte.libs.common.behandling.Hendelsestype
import no.nav.etterlatte.libs.common.behandling.Omberegningshendelse
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import org.slf4j.LoggerFactory

const val REGULERING_EVENT_NAME = "REGULERING" // TODO sj: Flyttes ut

internal class Reguleringsforespoersel(
    rapidsConnection: RapidsConnection,
    private val vedtak: VedtakService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(Reguleringsforespoersel::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(REGULERING_EVENT_NAME)
            validate { it.requireKey("sakId") }
            validate { it.requireKey("dato") }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            val sakId = packet["sakId"].asLong()
            logger.info("Leser reguleringsfoerespoersel for sak $sakId")

            val reguleringsdato = packet["dato"].asLocalDate()
            val respons = vedtak.harLoependeYtelserFra(sakId, reguleringsdato)
            respons.takeIf { it.erLoepende }?.let {
                packet.eventName = Hendelsestype.OMBEREGNINGSHENDELSE.toString()
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