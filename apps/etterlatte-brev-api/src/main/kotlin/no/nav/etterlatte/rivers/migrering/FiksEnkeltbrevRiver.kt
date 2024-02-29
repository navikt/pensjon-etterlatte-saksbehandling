package no.nav.etterlatte.rivers.migrering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.varselbrev.VarselbrevService
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.behandlingId
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rivers.FerdigstillJournalfoerOgDistribuerBrev
import no.nav.etterlatte.token.Systembruker
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

internal class FiksEnkeltbrevRiver(
    rapidsConnection: RapidsConnection,
    private val service: VarselbrevService,
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
            val varselbrev = service.hentVarselbrev(behandlingId).first()
            ferdigstillJournalfoerOgDistribuerBrev.journalfoerOgDistribuer(
                Brevkoder.BP_VARSEL,
                varselbrev.sakId,
                varselbrev.id,
                Systembruker.migrering,
            )
        }
        context.publish(packet.toJson())
    }
}

val behandlingerAaJournalfoereBrevFor =
    listOf(
        "ff703d71-89f4-4faf-8bf8-2860b3bfc27a",
        "543afe0a-6cf1-40a0-8c4e-283bb6ab7fb3",
    )
