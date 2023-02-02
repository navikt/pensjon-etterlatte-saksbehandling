package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.brev.BrevService
import no.nav.etterlatte.distribusjon.DistribusjonService
import no.nav.etterlatte.libs.common.brev.model.Adresse
import no.nav.etterlatte.libs.common.brev.model.BrevEventTypes
import no.nav.etterlatte.libs.common.distribusjon.BestillingsID
import no.nav.etterlatte.libs.common.distribusjon.DistribusjonsTidspunktType
import no.nav.etterlatte.libs.common.distribusjon.DistribusjonsType
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationIdKey
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class DistribuerBrev(
    private val rapidsConnection: RapidsConnection,
    private val distribusjonService: DistribusjonService,
    private val brevService: BrevService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(DistribuerBrev::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(BrevEventTypes.JOURNALFOERT.toString())
            validate { it.requireKey("brevId", "journalpostId", "distribusjonType", "mottakerAdresse") }
            validate { it.rejectKey("bestillingsId") }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet[correlationIdKey].asText()) {
            logger.info("Starter distribuering av brev.")

            val bestillingsId = distribusjonService.distribuerJournalpost(
                brevId = packet["brevId"].asLong(),
                journalpostId = packet["journalpostId"].asText(),
                type = packet.distribusjonType(),
                tidspunkt = DistribusjonsTidspunktType.KJERNETID,
                adresse = packet.mottakerAdresse()
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

    private fun JsonMessage.mottakerAdresse(): Adresse? = try {
        objectMapper.readValue(this["mottakerAdresse"].toJson())
    } catch (ex: Exception) {
        logger.error("Klarte ikke parse mottaker sin adresse:", ex)
        throw ex
    }

    private fun RapidsConnection.svarSuksess(packet: JsonMessage, bestillingsId: BestillingsID) {
        logger.info("Brev har blitt distribuert. Svarer tilbake med bekreftelse.")

        packet[eventNameKey] = BrevEventTypes.DISTRIBUERT.toString()
        packet["bestillingsId"] = bestillingsId

        publish(packet.toJson())
    }
}