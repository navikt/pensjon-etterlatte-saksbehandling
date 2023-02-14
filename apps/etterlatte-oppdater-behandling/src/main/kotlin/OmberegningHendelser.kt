package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.treeToValue
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.Hendelsestype
import no.nav.etterlatte.libs.common.behandling.Omberegningshendelse
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.*

internal class OmberegningHendelser(rapidsConnection: RapidsConnection, private val behandlinger: Behandling) :
    River.PacketListener {

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        logger.info("initierer rapid for omberegninghendelser")
        River(rapidsConnection).apply {
            eventName(Hendelsestype.OMBEREGNINGSHENDELSE.toString())

            correlationId()
            validate { it.rejectKey("omberegning") }
            validate { it.requireKey("hendelse_data") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            logger.info("Mottatt omberegninghendelse")
            try {
                val hendelse: Omberegningshendelse = objectMapper.treeToValue(packet["hendelse_data"])
                runBlocking {
                    val omberegningId = behandlinger.opprettOmberegning(hendelse).body<UUID>()
                    packet["omberegning"] = omberegningId
                    context.publish(packet.toJson())
                }
                logger.info("Publiserte oppdatert omberegningshendelse")
            } catch (e: Exception) {
                logger.error("Feil oppstod under lesing / sending av hendelse til behandling ", e)
            }
        }
    }
}