package no.nav.etterlatte.rivers

import no.nav.etterlatte.brev.Brevdistribuerer
import no.nav.etterlatte.brev.distribusjon.BestillingsID
import no.nav.etterlatte.brev.distribusjon.DistribusjonsType
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.migrering.ListenerMedLogging

internal class DistribuerBrevRiver(
    private val rapidsConnection: RapidsConnection,
    private val brevdistribuerer: Brevdistribuerer,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(DistribuerBrevRiver::class.java)

    init {
        initialiserRiver(rapidsConnection, BrevEventTypes.JOURNALFOERT.toString()) {
            validate { it.requireKey("brevId", "journalpostId", "distribusjonType") }
            validate { it.rejectKey("bestillingsId") }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val bestillingsId =
            brevdistribuerer.distribuerVedtaksbrev(
                packet["brevId"].asLong(),
                packet.distribusjonType(),
                packet["journalpostId"].asText(),
            )
        rapidsConnection.svarSuksess(packet, bestillingsId)
    }

    private fun JsonMessage.distribusjonType(): DistribusjonsType =
        try {
            DistribusjonsType.valueOf(this["distribusjonType"].asText())
        } catch (ex: Exception) {
            logger.error("Klarte ikke hente ut distribusjonstype:", ex)
            throw ex
        }

    private fun RapidsConnection.svarSuksess(
        packet: JsonMessage,
        bestillingsId: BestillingsID,
    ) {
        logger.info("Brev har blitt distribuert. Svarer tilbake med bekreftelse.")

        packet[EVENT_NAME_KEY] = BrevEventTypes.DISTRIBUERT.toString()
        packet["bestillingsId"] = bestillingsId

        publish(packet.toJson())
    }
}
