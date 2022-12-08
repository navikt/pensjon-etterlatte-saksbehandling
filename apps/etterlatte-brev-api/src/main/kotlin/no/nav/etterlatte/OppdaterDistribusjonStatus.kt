package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.libs.common.brev.model.BrevEventTypes.DISTRIBUERT
import no.nav.etterlatte.libs.common.brev.model.BrevEventTypes.JOURNALFOERT
import no.nav.etterlatte.libs.common.brev.model.Status
import no.nav.etterlatte.libs.common.journalpost.JournalpostResponse
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationIdKey
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class OppdaterDistribusjonStatus(rapidsConnection: RapidsConnection, private val db: BrevRepository) :
    River.PacketListener {
    private val logger = LoggerFactory.getLogger(OppdaterDistribusjonStatus::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandAny(eventNameKey, listOf(JOURNALFOERT.toString(), DISTRIBUERT.toString())) }
            validate { it.requireKey("brevId", correlationIdKey, "journalpostResponse") }
            validate { it.interestedIn("bestillingsId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            val brevId = packet["brevId"].longValue()
            logger.info("Mottatt oppdatering fra brev-distribusjon for brev med id $brevId.")

            if (packet[eventNameKey].asText() == DISTRIBUERT.toString()) {
                val bestillingsId = packet["bestillingsId"].asText()
                db.oppdaterStatus(brevId, Status.DISTRIBUERT, """{"bestillingsId": "$bestillingsId"}""")
                db.setBestillingsId(brevId, bestillingsId)
            } else {
                val response: JournalpostResponse = objectMapper.readValue(packet["journalpostResponse"].asText())
                db.oppdaterStatus(brevId, Status.JOURNALFOERT, response.toJson())
                db.setJournalpostId(brevId, response.journalpostId)
            }
        }
    }
}