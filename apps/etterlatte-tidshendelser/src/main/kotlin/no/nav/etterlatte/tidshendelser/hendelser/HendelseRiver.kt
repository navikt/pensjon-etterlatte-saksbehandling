package no.nav.etterlatte.tidshendelser.hendelser

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.TIDSHENDELSE_ID_KEY
import no.nav.etterlatte.rapidsandrivers.TIDSHENDELSE_STEG_KEY
import no.nav.etterlatte.rapidsandrivers.TIDSHENDELSE_TYPE_KEY
import no.nav.etterlatte.rapidsandrivers.sakId
import org.slf4j.LoggerFactory
import java.util.UUID

class HendelseRiver(
    rapidsConnection: RapidsConnection,
    private val hendelseDao: HendelseDao,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val avsluttendeSteg =
        listOf(
            "VEDTAK_ATTESTERT",
            "OPPGAVE_OPPRETTET",
            "HOPPET_OVER",
            "AKTIVITETSPLIKT_REVURDERING_OPPRETTET",
            "OPPDATERT_SAK",
        )

    init {
        initialiserRiver(rapidsConnection, EventNames.TIDSHENDELSE) {
            validate { it.requireKey(TIDSHENDELSE_STEG_KEY) }
            validate { it.requireKey(TIDSHENDELSE_TYPE_KEY) }
            validate { it.requireKey(TIDSHENDELSE_ID_KEY) }
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.interestedIn(HENDELSE_DATA_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val type = packet[TIDSHENDELSE_TYPE_KEY].asText()
        val steg = packet[TIDSHENDELSE_STEG_KEY].asText()
        val hendelseId = packet[TIDSHENDELSE_ID_KEY].asText()
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

            hendelseDao.oppdaterHendelseForSteg(hendelseIdUUID, steg, packet.toJson())

            if (steg == "VURDERT_LOEPENDE_YTELSE") {
                val loependeYtelse = packet[HENDELSE_DATA_KEY]["loependeYtelse"].asBoolean()
                logger.info("Sak $sakId har løpende ytelse? $loependeYtelse")
                hendelseDao.settHarLoependeYtelse(hendelseIdUUID, loependeYtelse)
            } else {
                if (steg in avsluttendeSteg) {
                    logger.info("Ferdigstiller hendelse $hendelseId for sak $sakId")
                    hendelseDao.oppdaterHendelseStatus(hendelseIdUUID, HendelseStatus.FERDIG)
                    hendelseDao.ferdigstillJobbHvisAlleHendelserErFerdige(hendelseIdUUID)
                }
            }
        }
    }
}
