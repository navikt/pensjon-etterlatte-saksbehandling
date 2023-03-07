package no.nav.etterlatte.rivers

import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.brev.model.Brev
import no.nav.etterlatte.libs.common.brev.model.BrevEventTypes
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.distribusjon.DistribusjonsType
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.KafkaHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class JournalfoerVedtaksbrev(
    private val rapidsConnection: RapidsConnection,
    private val service: VedtaksbrevService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(JournalfoerVedtaksbrev::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(KafkaHendelseType.ATTESTERT.toString())
            validate { it.requireKey("vedtak") }
            validate {
                it.rejectValues(
                    "vedtak.behandling.type",
                    listOf(BehandlingType.MANUELT_OPPHOER.name, BehandlingType.OMREGNING.name)
                )
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        try {
            withLogContext {
                val vedtak: VedtakDto = deserialize(packet["vedtak"].toJson())
                logger.info("Nytt vedtak med id ${vedtak.vedtakId} er attestert. Ferdigstiller vedtaksbrev.")

                val (brev, response) = service.journalfoerVedtaksbrev(vedtak)

                logger.info("Vedtaksbrev for vedtak med id ${vedtak.vedtakId} er journalfoert OK")

                rapidsConnection.svarSuksess(
                    packet,
                    brev,
                    response.journalpostId
                )
            }
        } catch (e: Exception) {
            logger.error("Feil ved ferdigstilling av vedtaksbrev: ", e)
            throw e
        }
    }

    private fun RapidsConnection.svarSuksess(
        packet: JsonMessage,
        brev: Brev,
        journalpostId: String
    ) {
        logger.info("Brev har blitt distribuert. Svarer tilbake med bekreftelse.")

        packet[EVENT_NAME_KEY] = BrevEventTypes.JOURNALFOERT.toString()
        packet["brevId"] = brev.id
        packet["journalpostId"] = journalpostId
        packet["distribusjonType"] = DistribusjonsType.VEDTAK.toString()
        packet["mottakerAdresse"] = brev.mottaker.adresse!!

        publish(packet.toJson())
    }
}