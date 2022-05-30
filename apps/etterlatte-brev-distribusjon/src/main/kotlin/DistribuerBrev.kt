package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.distribusjon.BestillingID
import no.nav.etterlatte.distribusjon.DistribusjonService
import no.nav.etterlatte.distribusjon.DistribusjonsTidspunktType
import no.nav.etterlatte.distribusjon.DistribusjonsType
import no.nav.etterlatte.libs.common.journalpost.JournalpostResponse
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class DistribuerBrev(
    private val rapidsConnection: RapidsConnection,
    private val distribusjonService: DistribusjonService,
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(DistribuerBrev::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event", "BREV:DISTRIBUER") }
            validate { it.requireKey("@brevId", "@correlation_id", "@journalpostResponse") }
            validate { it.rejectKey("@bestillingId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet["@correlation_id"].asText()) {
            logger.info("Starter distribuering av brev.")

            val bestillingId = distribusjonService.distribuerJournalpost(
                journalpostId = packet.journalpostId(),
                type = DistribusjonsType.VEDTAK, // todo: Støtte andre brev.
                tidspunkt = DistribusjonsTidspunktType.KJERNETID, // todo: dobbeltsjekk.
                adresse = null // todo: Støtte adresse. Og sjekke opp dist. til organiasjonsnummer.
            )

            rapidsConnection.svarSuksess(packet, bestillingId)
        }
    }

    private fun JsonMessage.journalpostId(): String = try {
        objectMapper.readValue<JournalpostResponse>(this["@journalpostResponse"].asText()).journalpostId
    } catch (ex: Exception) {
        logger.error("Klarte ikke hente ut journalpostId:", ex)
        throw ex
    }

    private fun RapidsConnection.svarSuksess(packet: JsonMessage, bestillingId: BestillingID) {
        logger.info("Brev har blitt distribuert. Svarer tilbake med bekreftelse.")

        packet["@bestillingId"] = bestillingId

        publish(packet.toJson())
    }
}
