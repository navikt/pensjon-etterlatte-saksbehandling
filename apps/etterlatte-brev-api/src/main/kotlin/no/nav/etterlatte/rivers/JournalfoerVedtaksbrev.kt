package no.nav.etterlatte.rivers

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.brev.distribusjon.DistribusjonsType
import no.nav.etterlatte.brev.model.BrevEventTypes
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.SKAL_SENDE_BREV
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.KafkaHendelseType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.migrering.ListenerMedLogging
import java.util.*

internal class JournalfoerVedtaksbrev(
    private val rapidsConnection: RapidsConnection,
    private val service: VedtaksbrevService
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(JournalfoerVedtaksbrev::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(KafkaHendelseType.ATTESTERT.toString())
            validate { it.requireKey("vedtak") }
            validate { it.requireKey("vedtak.vedtakId") }
            validate { it.requireKey("vedtak.behandling.id") }
            validate { it.requireKey("vedtak.sak") }
            validate { it.requireKey("vedtak.sak.id") }
            validate { it.requireKey("vedtak.sak.ident") }
            validate { it.requireKey("vedtak.sak.sakType") }
            validate { it.requireKey("vedtak.vedtakFattet.ansvarligEnhet") }
            validate {
                it.rejectValues("vedtak.behandling.type", listOf(BehandlingType.MANUELT_OPPHOER.name))
            }
            validate { it.rejectValue(SKAL_SENDE_BREV, false) }
        }.register(this)
    }

    override fun haandterPakke(packet: JsonMessage, context: MessageContext) {
        try {
            val vedtak = VedtakTilJournalfoering(
                vedtakId = packet["vedtak.vedtakId"].asLong(),
                sak = deserialize(packet["vedtak.sak"].toJson()),
                behandlingId = UUID.fromString(packet["vedtak.behandling.id"].asText()),
                ansvarligEnhet = packet["vedtak.vedtakFattet.ansvarligEnhet"].asText()
            )
            logger.info("Nytt vedtak med id ${vedtak.vedtakId} er attestert. Ferdigstiller vedtaksbrev.")
            val behandlingId = vedtak.behandlingId

            val vedtaksbrev = service.hentVedtaksbrev(behandlingId)
                ?: throw NoSuchElementException("Ingen vedtaksbrev funnet på behandlingId=$behandlingId")

            // TODO: Forbedre denne "fiksen". Gjøres nå for å lappe sammen
            if (vedtaksbrev.status == Status.JOURNALFOERT) {
                logger.warn("Vedtaksbrev (id=${vedtaksbrev.id}) er allerede journalført.")
                return
            }

            val response = service.journalfoerVedtaksbrev(vedtaksbrev, vedtak)

            logger.info("Vedtaksbrev for vedtak med id ${vedtak.vedtakId} er journalfoert OK")

            rapidsConnection.svarSuksess(
                packet,
                vedtaksbrev.id,
                response.journalpostId
            )
        } catch (e: Exception) {
            logger.error("Feil ved ferdigstilling av vedtaksbrev: ", e)
            throw e
        }
    }

    private fun RapidsConnection.svarSuksess(
        packet: JsonMessage,
        brevId: BrevID,
        journalpostId: String
    ) {
        logger.info("Brev har blitt distribuert. Svarer tilbake med bekreftelse.")

        packet[EVENT_NAME_KEY] = BrevEventTypes.JOURNALFOERT.toString()
        packet["brevId"] = brevId
        packet["journalpostId"] = journalpostId
        packet["distribusjonType"] = DistribusjonsType.VEDTAK.toString()

        publish(packet.toJson())
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class VedtakTilJournalfoering(
    val vedtakId: Long,
    val sak: VedtakSak,
    val behandlingId: UUID,
    val ansvarligEnhet: String
)