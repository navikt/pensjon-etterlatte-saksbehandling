package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.treeToValue
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.Omregningshendelse
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.EventNames.BEREGN
import no.nav.etterlatte.rapidsandrivers.EventNames.OMREGNINGSHENDELSE
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.OMREGNING_ID_KEY
import rapidsandrivers.omregningId
import rapidsandrivers.withFeilhaandtering
import java.util.*

internal class OmregningsHendelser(rapidsConnection: RapidsConnection, private val behandlinger: Behandling) :
    River.PacketListener {

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        logger.info("initierer rapid for omregningshendelser")
        River(rapidsConnection).apply {
            eventName(OMREGNINGSHENDELSE)

            correlationId()
            validate { it.rejectKey(OMREGNING_ID_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, OMREGNINGSHENDELSE) {
                logger.info("Mottatt omregningshendelse")

                val hendelse: Omregningshendelse = objectMapper.treeToValue(packet[HENDELSE_DATA_KEY])
                runBlocking {
                    val behandling = behandlinger.opprettOmregning(hendelse).body<UUID>()
                    packet.omregningId = behandling
                    packet.eventName = BEREGN
                    context.publish(packet.toJson())
                }
                logger.info("Publiserte oppdatert omregningshendelse")
            }
        }
    }
}