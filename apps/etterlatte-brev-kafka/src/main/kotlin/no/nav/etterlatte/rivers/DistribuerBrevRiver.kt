package no.nav.etterlatte.rivers

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.BrevHendelseType
import no.nav.etterlatte.klienter.BrevapiKlient
import no.nav.etterlatte.libs.common.brev.BestillingsIdDto
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.rapidsandrivers.BREV_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
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
            validate { it.requireKey(BREV_ID_KEY, "journalpostId", "distribusjonType") }
            validate { it.rejectKey("bestillingsId") }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val bestillingsId =
            runBlocking {
                brevapiKlient.distribuer(
                    brevId = packet[BREV_ID_KEY].asLong(),
                    distribusjonsType = packet.hentVerdiEllerKastFeil("distribusjonType"),
                    journalpostIdInn = packet["journalpostId"].asText(),
                )
            }
        rapidsConnection.svarSuksess(packet, bestillingsId)
    }

    /*
    TODO: vurdere å flytte distribusjosnmodell til modell
    private fun JsonMessage.distribusjonType(): DistribusjonsType =
        try {
            DistribusjonsType.valueOf(this["distribusjonType"].asText())
        } catch (ex: Exception) {
            logger.error("Klarte ikke hente ut distribusjonstype:", ex)
            throw ex
        }
*/

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

private fun JsonMessage.hentVerdiEllerKastFeil(key: String): String {
    val verdi = this[key].toString()
    if (verdi.isEmpty()) {
        throw RuntimeException("Må ha verdi for key $key")
    } else {
        return verdi
    }
}
