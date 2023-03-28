package no.nav.etterlatte.rivers

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.brev.model.Brev
import no.nav.etterlatte.libs.common.brev.model.BrevEventTypes
import no.nav.etterlatte.libs.common.brev.model.Status
import no.nav.etterlatte.libs.common.distribusjon.DistribusjonsType
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.SKAL_SENDE_BREV
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.vedtak.KafkaHendelseType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.UUID

internal class JournalfoerVedtaksbrev(
    private val rapidsConnection: RapidsConnection,
    private val service: VedtaksbrevService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(JournalfoerVedtaksbrev::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(KafkaHendelseType.ATTESTERT.toString())
            validate { it.requireKey("vedtak") }
            validate { it.requireKey("vedtak.vedtakId") }
            validate { it.requireKey("vedtak.behandling.id") }
            validate { it.requireKey("vedtak.sak.ident") }
            validate { it.requireKey("vedtak.vedtakFattet.ansvarligEnhet") }
            validate {
                it.rejectValues("vedtak.behandling.type", listOf(BehandlingType.MANUELT_OPPHOER.name))
            }
            validate { it.rejectValue(SKAL_SENDE_BREV, false) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        try {
            withLogContext {
                val vedtak = VedtakTilJournalfoering(
                    vedtakId = packet["vedtak.vedtakId"].asLong(),
                    behandlingId = UUID.fromString(packet["vedtak.behandling.id"].asText()),
                    soekerIdent = packet["vedtak.sak.ident"].asText(),
                    ansvarligEnhet = packet["vedtak.vedtakFattet.ansvarligEnhet"].asText()
                )
                logger.info("Nytt vedtak med id ${vedtak.vedtakId} er attestert. Ferdigstiller vedtaksbrev.")
                val behandlingId = vedtak.behandlingId

                val vedtaksbrev = service.hentVedtaksbrev(behandlingId)
                    ?: throw NoSuchElementException("Ingen vedtaksbrev funnet på behandlingId=$behandlingId")

                // TODO: Forbedre denne "fiksen". Gjøres nå for å lappe sammen
                if (vedtaksbrev.status == Status.JOURNALFOERT) {
                    logger.warn("Vedtaksbrev (id=${vedtaksbrev.id}) er allerede journalført.")
                    return@withLogContext
                }

                val (brev, response) = service.journalfoerVedtaksbrev(vedtaksbrev, vedtak)

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

@JsonIgnoreProperties(ignoreUnknown = true)
data class VedtakTilJournalfoering(
    val vedtakId: Long,
    val behandlingId: UUID,
    val soekerIdent: String,
    val ansvarligEnhet: String
)