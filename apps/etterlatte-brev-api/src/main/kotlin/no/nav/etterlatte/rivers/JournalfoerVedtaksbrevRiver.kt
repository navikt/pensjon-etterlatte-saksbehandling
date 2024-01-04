package no.nav.etterlatte.rivers

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.JournalfoerBrevService
import no.nav.etterlatte.brev.distribusjon.DistribusjonsType
import no.nav.etterlatte.brev.model.BrevID
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
    private val journalfoerBrevService: JournalfoerBrevService,
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

            val response = runBlocking { journalfoerBrevService.journalfoerVedtaksbrev(vedtak) } ?: return
            rapidsConnection.svarSuksess(
                packet,
                response.second,
                response.first.journalpostId,
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
