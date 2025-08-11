package no.nav.etterlatte.rivers

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.BrevHendelseType
import no.nav.etterlatte.brev.distribusjon.DistribusjonsType
import no.nav.etterlatte.klienter.BrevapiKlient
import no.nav.etterlatte.libs.common.brev.BestillingsIdDto
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.rapidsandrivers.BREV_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import org.slf4j.LoggerFactory

internal class DistribuerBrevRiver(
    private val rapidsConnection: RapidsConnection,
    private val brevapiKlient: BrevapiKlient,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(DistribuerBrevRiver::class.java)

    init {
        initialiserRiver(rapidsConnection, BrevHendelseType.JOURNALFOERT) {
            validate { it.requireKey(BREV_ID_KEY, "distribusjonType") }
            validate { it.requireKey("vedtak.sak.id") }
            validate { it.rejectKey("bestillingsId") }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val sakid = SakId(packet["vedtak.sak.id"].asLong())

        val bestillingsId =
            runBlocking {
                brevapiKlient.distribuer(
                    brevId = packet[BREV_ID_KEY].asLong(),
                    distribusjonsType = packet.distribusjonType(),
                    sakId = sakid,
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
