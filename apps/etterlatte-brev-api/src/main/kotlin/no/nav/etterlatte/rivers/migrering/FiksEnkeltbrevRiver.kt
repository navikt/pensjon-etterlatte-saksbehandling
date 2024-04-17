package no.nav.etterlatte.rivers.migrering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.MigreringBrevRequest
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.behandlingId
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rivers.FerdigstillJournalfoerOgDistribuerBrev
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

internal class FiksEnkeltbrevRiver(
    rapidsConnection: RapidsConnection,
    private val service: VedtaksbrevService,
    private val ferdigstillJournalfoerOgDistribuerBrev: FerdigstillJournalfoerOgDistribuerBrev,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Migreringshendelser.FIKS_ENKELTBREV) {
            validate { it.requireKey(BEHANDLING_ID_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = packet.behandlingId
        logger.info("Fikser vedtaksbrev for behandling $behandlingId")

        runBlocking {
            val bruker = Systembruker.migrering
            val vedtaksbrev = requireNotNull(service.hentVedtaksbrev(behandlingId))
            logger.info("Henta brev ${vedtaksbrev.id} for ferdigstilling og utsending")
            service.genererPdf(
                id = vedtaksbrev.id,
                bruker = bruker,
                automatiskMigreringRequest = MigreringBrevRequest(0, true, null),
            )
            logger.info("Genererte pdf for $vedtaksbrev.id")
            service.ferdigstillVedtaksbrev(vedtaksbrev.behandlingId!!, bruker, true)
            logger.info("Ferdigstilte $vedtaksbrev.id, klar til journalføring")
            ferdigstillJournalfoerOgDistribuerBrev.journalfoerOgDistribuer(
                Brevkoder.BP_INNVILGELSE,
                vedtaksbrev.sakId,
                vedtaksbrev.id,
                bruker,
            )
        }
        context.publish(packet.toJson())
    }

    override fun kontekst() = Kontekst.MIGRERING
}

val behandlingerAaJournalfoereBrevFor =
    listOf(
        "6f11c98e-b6d8-4fa2-804a-ff2e375dabb3",
    )
