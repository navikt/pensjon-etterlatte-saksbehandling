package no.nav.etterlatte.rivers.migrering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.varselbrev.VarselbrevService
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.behandlingId
import no.nav.etterlatte.rapidsandrivers.migrering.KILDE_KEY
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.etterlatte.rivers.OpprettFerdigstillJournalfoerOgDistribuerBrev
import no.nav.etterlatte.token.Systembruker
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

internal class OpprettVedtaksbrevForGjenopprettaRiver(
    rapidsConnection: RapidsConnection,
    private val service: VarselbrevService,
    private val opprettFerdigstillJournalfoerOgDistribuerBrev: OpprettFerdigstillJournalfoerOgDistribuerBrev,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, VedtakKafkaHendelseHendelseType.FATTET) {
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(BEHANDLING_ID_KEY) }
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
            val vedtaksbrev = service.opprettVarselbrev(sakId, behandlingId, brukerTokenInfo)
            val brevkoder = service.hentBrevkodeForSak(sakId, brukerTokenInfo)
            opprettFerdigstillJournalfoerOgDistribuerBrev.ferdigstillOgGenererPDF(
                brevkoder,
                sakId,
                vedtaksbrev,
                brukerTokenInfo,
            )
            opprettFerdigstillJournalfoerOgDistribuerBrev.journalfoerOgDistribuer(
                brevkoder,
                sakId,
                vedtaksbrev.first.id,
                brukerTokenInfo,
            )
        }
    }
}
