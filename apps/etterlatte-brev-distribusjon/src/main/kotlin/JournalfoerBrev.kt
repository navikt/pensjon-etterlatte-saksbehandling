package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.readValue
import journalpost.JournalpostService
import no.nav.etterlatte.libs.common.brev.model.DistribusjonMelding
import no.nav.etterlatte.libs.common.journalpost.JournalpostResponse
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class JournalfoerBrev(
    private val rapidsConnection: RapidsConnection,
    private val journalpostService: JournalpostService,
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(JournalfoerBrev::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event", "BREV:DISTRIBUER") }
            validate { it.requireKey("@brevId", "payload") }
            validate { it.rejectKey("@bestilling_id", "@journalpostResponse") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext {
            logger.info("Starter journalføring av brev.")

            val melding = packet.distribusjonsmelding()
            val journalpostResponse = journalpostService.journalfoer(melding)

            rapidsConnection.svarSuksess(packet, journalpostResponse)
        }
    }

    private fun JsonMessage.distribusjonsmelding(): DistribusjonMelding = try {
        objectMapper.readValue(this["payload"].toString())
    } catch (ex: Exception) {
        logger.error("Klarte ikke parse distribusjonsmeldingen:", ex)
        throw ex
    }

    private fun RapidsConnection.svarSuksess(packet: JsonMessage, journalpostResponse: JournalpostResponse) {
        logger.info("Brev har blitt journalført. Svarer tilbake med bekreftelse.")

        packet["@journalpostResponse"] = journalpostResponse.toJson()

        publish(packet.toJson())
    }
}
