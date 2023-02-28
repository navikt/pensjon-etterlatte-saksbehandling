package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.treeToValue
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.Omberegningshendelse
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.EventNames.BEREGN
import no.nav.etterlatte.rapidsandrivers.EventNames.OMBEREGNINGSHENDELSE
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.OMBEREGNING_ID_KEY
import rapidsandrivers.omberegningId
import java.util.*

internal class OmberegningsHendelser(rapidsConnection: RapidsConnection, private val behandlinger: Behandling) :
    River.PacketListener {

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        logger.info("initierer rapid for omberegningshendelser")
        River(rapidsConnection).apply {
            eventName(OMBEREGNINGSHENDELSE)

            correlationId()
            validate { it.rejectKey(OMBEREGNING_ID_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            logger.info("Mottatt omberegningshendelse")
            try {
                val hendelse: Omberegningshendelse = objectMapper.treeToValue(packet[HENDELSE_DATA_KEY])
                runBlocking {
                    val behandling = behandlinger.opprettOmberegning(hendelse).body<UUID>()
                    packet.omberegningId = behandling
                    packet.eventName = BEREGN
                    context.publish(packet.toJson())
                }
                logger.info("Publiserte oppdatert omberegningshendelse")
            } catch (e: Exception) {
                logger.error("Feil oppstod under lesing / sending av hendelse til behandling ", e)
            }
        }
    }
}