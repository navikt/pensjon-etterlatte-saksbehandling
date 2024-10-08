package no.nav.etterlatte.rivers

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.BrevHendelseType
import no.nav.etterlatte.brev.distribusjon.DistribusjonsType
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.klienter.BrevapiKlient
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.rapidsandrivers.SKAL_SENDE_BREV
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.rapidsandrivers.BREV_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.util.UUID

internal class JournalfoerVedtaksbrevRiver(
    private val rapidsConnection: RapidsConnection,
    private val brevapiKlient: BrevapiKlient,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(JournalfoerVedtaksbrevRiver::class.java)

    init {
        initialiserRiver(rapidsConnection, VedtakKafkaHendelseHendelseType.ATTESTERT) {
            validate { it.requireKey("vedtak") }
            validate { it.requireKey("vedtak.id") }
            validate { it.requireKey("vedtak.behandlingId") }
            validate { it.requireKey("vedtak.sak") }
            validate { it.requireKey("vedtak.sak.id") }
            validate { it.requireKey("vedtak.sak.ident") }
            validate { it.requireKey("vedtak.sak.sakType") }
            validate { it.requireKey("vedtak.vedtakFattet.ansvarligSaksbehandler") }
            validate { it.requireKey("vedtak.vedtakFattet.ansvarligEnhet") }
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
                    behandlingId = hentBehandlingId(packet),
                    ansvarligEnhet = Enhetsnummer(packet["vedtak.vedtakFattet.ansvarligEnhet"].asText()),
                    saksbehandler = packet["vedtak.vedtakFattet.ansvarligSaksbehandler"].asText(),
                )

            val journalfoervedtaksbrevResponse = runBlocking { brevapiKlient.journalfoerVedtaksbrev(vedtak) }
            if (journalfoervedtaksbrevResponse == null) {
                logger.warn("Jorunalføring ble ikke journalført, kan være pga status eller migrering.")
            } else {
                rapidsConnection.svarSuksess(
                    packet,
                    journalfoervedtaksbrevResponse.brevId,
                    journalfoervedtaksbrevResponse.opprettetJournalpost.journalpostId,
                    vedtak,
                )
            }
        } catch (e: Exception) {
            logger.error("Feil ved journalføring av vedtaksbrev: ", e)
            throw e
        }
    }

    private fun hentBehandlingId(packet: JsonMessage) = UUID.fromString(packet["vedtak.behandlingId"].asText())

    private fun RapidsConnection.svarSuksess(
        packet: JsonMessage,
        brevId: BrevID,
        journalpostId: String,
        vedtakTilJournalfoering: VedtakTilJournalfoering,
    ) {
        logger.info("Brev har blitt distribuert. Svarer tilbake med bekreftelse.")
        packet.setEventNameForHendelseType(BrevHendelseType.JOURNALFOERT)
        packet[BREV_ID_KEY] = brevId
        packet[SAK_ID_KEY] = vedtakTilJournalfoering.sak.id
        packet["journalpostId"] = journalpostId
        packet["distribusjonType"] =
            DistribusjonsType.VEDTAK.name

        publish(packet.toJson())
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class VedtakTilJournalfoering(
    val vedtakId: Long,
    val sak: VedtakSak,
    val behandlingId: UUID,
    val ansvarligEnhet: Enhetsnummer,
    val saksbehandler: String,
)
