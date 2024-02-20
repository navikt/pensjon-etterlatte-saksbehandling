package no.nav.etterlatte.rivers.migrering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.adresse.AvsenderRequest
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.Brevkoder
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.varselbrev.VarselbrevService
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.OPPGAVE_KEY
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.behandlingId
import no.nav.etterlatte.rapidsandrivers.migrering.KILDE_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.etterlatte.rivers.FerdigstillJournalfoerOgDistribuerBrev
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Fagsaksystem
import no.nav.etterlatte.token.Systembruker
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.util.UUID

internal class OpprettVarselbrevForGjenopprettaRiver(
    rapidsConnection: RapidsConnection,
    private val service: VarselbrevService,
    private val ferdigstillJournalfoerOgDistribuerBrev: FerdigstillJournalfoerOgDistribuerBrev,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Migreringshendelser.BEREGNET_FERDIG) {
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(OPPGAVE_KEY) }
            validate { it.requireValue(KILDE_KEY, Vedtaksloesning.GJENOPPRETTA.name) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val sakId = packet.sakId
        logger.info("Oppretter varselbrev i sak $sakId")
        val behandlingId = packet.behandlingId
        val brukerTokenInfo = Systembruker.migrering
        runBlocking {
            opprettOgSendUtBrev(sakId, behandlingId, brukerTokenInfo)
            // Her skal vi sette på vent
        }
    }

    private suspend fun opprettOgSendUtBrev(
        sakId: Long,
        behandlingId: UUID,
        brukerTokenInfo: Systembruker,
    ) {
        val varselbrev = service.opprettVarselbrev(sakId, behandlingId, brukerTokenInfo)
        ferdigstillOgGenererPDF(
            varselbrev.brevkoder,
            sakId,
            varselbrev.let { Pair(it.brev, it.generellBrevData) },
            brukerTokenInfo,
        )
        ferdigstillJournalfoerOgDistribuerBrev.journalfoerOgDistribuer(
            varselbrev.brevkoder,
            sakId,
            varselbrev.brev.id,
            brukerTokenInfo,
        )
    }

    private suspend fun ferdigstillOgGenererPDF(
        brevKode: Brevkoder,
        sakId: Long,
        brevOgData: Pair<Brev, GenerellBrevData>,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevID {
        logger.info("Ferdigstiller $brevKode-brev i sak $sakId")
        val brevId = brevOgData.first.id
        retryOgPakkUt {
            service.ferdigstillOgGenererPDF(
                brevId = brevId,
                bruker = brukerTokenInfo,
                avsenderRequest = { _, _ ->
                    AvsenderRequest(
                        saksbehandlerIdent = Fagsaksystem.EY.navn,
                        sakenhet = brevOgData.second.sak.enhet,
                        attestantIdent = Fagsaksystem.EY.navn,
                    )
                },
            )
        }
        return brevId
    }
}
