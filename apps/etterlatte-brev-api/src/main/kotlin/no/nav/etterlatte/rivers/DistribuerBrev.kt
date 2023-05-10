package no.nav.etterlatte.rivers

import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.brev.distribusjon.BestillingsID
import no.nav.etterlatte.brev.distribusjon.DistribusjonService
import no.nav.etterlatte.brev.distribusjon.DistribusjonsTidspunktType
import no.nav.etterlatte.brev.distribusjon.DistribusjonsType
import no.nav.etterlatte.brev.model.BrevEventTypes
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class DistribuerBrev(
    private val rapidsConnection: RapidsConnection,
    private val vedtaksbrevService: VedtaksbrevService,
    private val distribusjonService: DistribusjonService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(DistribuerBrev::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(BrevEventTypes.JOURNALFOERT.toString())
            validate { it.requireKey("brevId", "journalpostId", "distribusjonType") }
            validate { it.rejectKey("bestillingsId") }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet[CORRELATION_ID_KEY].asText()) {
            logger.info("Starter distribuering av brev.")

            val brev = vedtaksbrevService.hentBrev(packet["brevId"].asLong())

            val bestillingsId = distribusjonService.distribuerJournalpost(
                brevId = brev.id,
                journalpostId = packet["journalpostId"].asText(),
                type = packet.distribusjonType(),
                tidspunkt = DistribusjonsTidspunktType.KJERNETID,
                adresse = brev.mottaker.adresse
            )

            rapidsConnection.svarSuksess(packet, bestillingsId)
        }
    }

    private fun JsonMessage.distribusjonType(): DistribusjonsType = try {
        DistribusjonsType.valueOf(this["distribusjonType"].asText())
    } catch (ex: Exception) {
        logger.error("Klarte ikke hente ut distribusjonstype:", ex)
        throw ex
    }

    private fun RapidsConnection.svarSuksess(packet: JsonMessage, bestillingsId: BestillingsID) {
        logger.info("Brev har blitt distribuert. Svarer tilbake med bekreftelse.")

        packet[EVENT_NAME_KEY] = BrevEventTypes.DISTRIBUERT.toString()
        packet["bestillingsId"] = bestillingsId

        publish(packet.toJson())
    }
}