import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.db.BrevRepository
import no.nav.etterlatte.db.Status
import no.nav.etterlatte.libs.common.journalpost.JournalpostResponse
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
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
            validate { it.demandValue("@event", "BREV:DISTRIBUER") }
            validate { it.requireKey("@brevId", "@journalfoert") }
            validate { it.interestedIn("@journalpostResponse", "@distribuert")}
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val brevId = packet["@brevId"].longValue()
        withLogContext(brevId.toString()) {
            logger.info("Mottatt oppdatering fra brev-distribusjon for brev med id ${brevId}.")

            if (packet["@distribuert"].booleanValue()) {
                db.oppdaterStatus(brevId, Status.DISTRIBUERT, """{"id": "todo"}""")
                db.setBestillingId(brevId, "todo")
            } else if (packet["@journalfoert"].booleanValue()) {
                val response: JournalpostResponse = objectMapper.readValue(packet["@journalpostResponse"].asText())
                db.oppdaterStatus(brevId, Status.JOURNALFOERT, response.toJson())
                db.setJournalpostId(brevId, response.journalpostId)
            }
        }
    }
}
