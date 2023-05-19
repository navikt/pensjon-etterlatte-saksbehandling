import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.EventNames.TRYGDETID
import no.nav.etterlatte.trygdetid.kafka.TrygdetidService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.toUUID
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.BEHANDLING_VI_OMREGNER_FRA_KEY
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.withFeilhaandtering

internal class BeregnTrygdetid(rapidsConnection: RapidsConnection, private val trygdetidService: TrygdetidService) :
    River.PacketListener {
    private val logger = LoggerFactory.getLogger(BeregnTrygdetid::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(TRYGDETID)
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(BEHANDLING_VI_OMREGNER_FRA_KEY) }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, TRYGDETID) {
                val behandlingId = packet.behandlingId
                val behandlingViOmregnerFra = packet[BEHANDLING_VI_OMREGNER_FRA_KEY].asText().toUUID()

                trygdetidService.regulerTrygdetid(behandlingId, behandlingViOmregnerFra)

                packet.eventName = EventNames.BEREGN
                context.publish(packet.toJson())
                logger.info(
                    "Trygdetid er opprettet for behandling $behandlingId og melding beregningsmelding ble sendt."
                )
            }
        }
}