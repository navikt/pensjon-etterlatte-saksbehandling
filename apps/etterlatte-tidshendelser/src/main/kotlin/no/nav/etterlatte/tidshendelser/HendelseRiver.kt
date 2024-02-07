package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_STEG_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_TYPE_KEY
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.util.UUID

class HendelseRiver(
    rapidsConnection: RapidsConnection,
    private val hendelseDao: HendelseDao,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, EventNames.ALDERSOVERGANG) {
            validate { it.requireKey(ALDERSOVERGANG_STEG_KEY) }
            validate { it.requireKey(ALDERSOVERGANG_TYPE_KEY) }
            validate { it.requireKey(ALDERSOVERGANG_ID_KEY) }
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.interestedIn("data") }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val type = packet[ALDERSOVERGANG_TYPE_KEY].asText()
        val steg = packet[ALDERSOVERGANG_STEG_KEY].asText()
        val hendelseId = packet[ALDERSOVERGANG_ID_KEY].asText()
        val hendelseIdUUID = UUID.fromString(hendelseId)
        val sakId = packet.sakId

        withLogContext(
            correlationId = getCorrelationId(),
            mapOf(
                "hendelseId" to hendelseId,
                "sakId" to sakId.toString(),
                "type" to type,
                "steg" to steg,
            ),
        ) {
            logger.info("Behandler melding [hendelseId=$hendelseId, sak=$sakId, type=$type, step=$steg]")
            hendelseDao.oppdaterHendelseForSteg(hendelseIdUUID, steg)

            if (steg == "VURDERT_LOEPENDE_YTELSE") {
                val loependeYtelse = packet["data"].asBoolean()
                logger.info("Sak $sakId har løpende ytelse? $loependeYtelse")
            }
        }
    }
}
