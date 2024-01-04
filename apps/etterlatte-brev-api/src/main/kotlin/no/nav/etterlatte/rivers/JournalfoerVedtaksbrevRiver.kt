package no.nav.etterlatte.rivers

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.distribusjon.DistribusjonsType
import no.nav.etterlatte.brev.dokarkiv.DokarkivService
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.SKAL_SENDE_BREV
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.migrering.ListenerMedLogging
import java.util.UUID

internal class JournalfoerVedtaksbrevRiver(
    private val rapidsConnection: RapidsConnection,
    private val service: VedtaksbrevService,
    private val db: BrevRepository,
    private val dokarkivService: DokarkivService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(JournalfoerVedtaksbrevRiver::class.java)

    init {
        initialiserRiver(rapidsConnection, VedtakKafkaHendelseType.ATTESTERT.toString()) {
            validate { it.requireKey("vedtak") }
            validate { it.requireKey("vedtak.id") }
            validate { it.requireKey("vedtak.behandlingId") }
            validate { it.requireKey("vedtak.sak") }
            validate { it.requireKey("vedtak.sak.id") }
            validate { it.requireKey("vedtak.sak.ident") }
            validate { it.requireKey("vedtak.sak.sakType") }
            validate { it.requireKey("vedtak.vedtakFattet.ansvarligSaksbehandler") }
            validate { it.requireKey("vedtak.vedtakFattet.ansvarligEnhet") }
            validate {
                it.rejectValues("vedtak.innhold.behandling.type", listOf(BehandlingType.MANUELT_OPPHOER.name))
            }
            validate { it.rejectValue(SKAL_SENDE_BREV, false) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        try {
            val vedtak =
                VedtakTilJournalfoering(
                    vedtakId = packet["vedtak.id"].asLong(),
                    sak = deserialize(packet["vedtak.sak"].toJson()),
                    behandlingId = hentBehandling(packet),
                    ansvarligEnhet = packet["vedtak.vedtakFattet.ansvarligEnhet"].asText(),
                )

            logger.info("Nytt vedtak med id ${vedtak.vedtakId} er attestert. Ferdigstiller vedtaksbrev.")
            val behandlingId = vedtak.behandlingId

            val vedtaksbrev =
                service.hentVedtaksbrev(behandlingId)
                    ?: throw NoSuchElementException("Ingen vedtaksbrev funnet på behandlingId=$behandlingId")

            // TODO: Forbedre denne "fiksen". Gjøres nå for å lappe sammen
            if (vedtaksbrev.status in listOf(Status.JOURNALFOERT, Status.DISTRIBUERT, Status.SLETTET)) {
                logger.warn("Vedtaksbrev (id=${vedtaksbrev.id}) er allerede ${vedtaksbrev.status}.")
                return
            }

            val response =
                try {
                    if (vedtaksbrev.status != Status.FERDIGSTILT) {
                        throw IllegalArgumentException("Ugyldig status ${vedtaksbrev.status} på vedtaksbrev (id=${vedtaksbrev.id})")
                    }

                    val journalfoeringResponse = runBlocking { dokarkivService.journalfoer(vedtaksbrev.id, vedtak) }

                    db.settBrevJournalfoert(vedtaksbrev.id, journalfoeringResponse)
                    logger.info("Brev med id=${vedtaksbrev.id} markert som journalført")
                    journalfoeringResponse
                } catch (e: Exception) {
                    logger.error("Feila på å journalføre brev ${vedtaksbrev.id}")
                    throw e
                }

            logger.info("Vedtaksbrev for vedtak med id ${vedtak.vedtakId} er journalfoert OK")

            rapidsConnection.svarSuksess(
                packet,
                vedtaksbrev.id,
                response.journalpostId,
            )
        } catch (e: Exception) {
            logger.error("Feil ved ferdigstilling av vedtaksbrev: ", e)
            throw e
        }
    }

    private fun hentBehandling(packet: JsonMessage) = UUID.fromString(packet["vedtak.behandlingId"].asText())

    private fun RapidsConnection.svarSuksess(
        packet: JsonMessage,
        brevId: BrevID,
        journalpostId: String,
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
    val ansvarligEnhet: String,
)
