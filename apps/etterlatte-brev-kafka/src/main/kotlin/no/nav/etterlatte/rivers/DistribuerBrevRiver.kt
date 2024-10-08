package no.nav.etterlatte.rivers

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.BrevHendelseType
import no.nav.etterlatte.brev.distribusjon.DistribusjonsType
import no.nav.etterlatte.klienter.BrevapiKlient
import no.nav.etterlatte.libs.common.brev.BestillingsIdDto
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.rapidsandrivers.BREV_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

internal class DistribuerBrevRiver(
    private val rapidsConnection: RapidsConnection,
    private val brevapiKlient: BrevapiKlient,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(DistribuerBrevRiver::class.java)

    init {
        initialiserRiver(rapidsConnection, BrevHendelseType.JOURNALFOERT) {
            validate { it.requireKey(BREV_ID_KEY, "journalpostId", "distribusjonType", SAK_ID_KEY) }
            validate { it.requireKey("vedtak.sak.id") }
            validate { it.rejectKey("bestillingsId") }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val sakid = {
            val vedtakSakId = packet["vedtak.sak.id"].asLong()
            if (vedtakSakId != 0L) {
                vedtakSakId
            } else {
                packet.sakId
            }
        }

        val bestillingsId =
            runBlocking {
                brevapiKlient.distribuer(
                    brevId = packet[BREV_ID_KEY].asLong(),
                    distribusjonsType = packet.distribusjonType(),
                    journalpostIdInn = packet["journalpostId"].asText(),
                    sakId = sakid(),
                )
            }
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
        bestillingsIdDto: BestillingsIdDto,
    ) {
        logger.info("Brev har blitt distribuert. Svarer tilbake med bekreftelse.")
        packet.setEventNameForHendelseType(BrevHendelseType.DISTRIBUERT)
        packet["bestillingsId"] = bestillingsIdDto.bestillingsId

        publish(packet.toJson())
    }
}
