package no.nav.etterlatte.rivers

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.BREVMAL_RIVER_KEY
import no.nav.etterlatte.brev.BrevRequestHendelseType
import no.nav.etterlatte.brev.Brevoppretter
import no.nav.etterlatte.brev.JournalfoerBrevService
import no.nav.etterlatte.brev.PDFGenerator
import no.nav.etterlatte.brev.adresse.AvsenderRequest
import no.nav.etterlatte.brev.brevbaker.Brevkoder
import no.nav.etterlatte.brev.distribusjon.Brevdistribuerer
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Fagsaksystem
import no.nav.etterlatte.token.Systembruker
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

class OpprettJournalfoerOgDistribuerRiver(
    rapidsConnection: RapidsConnection,
    private val brevoppretter: Brevoppretter,
    private val pdfGenerator: PDFGenerator,
    private val journalfoerBrevService: JournalfoerBrevService,
    private val brevdistribuerer: Brevdistribuerer,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, BrevRequestHendelseType.OPPRETT_JOURNALFOER_OG_DISTRIBUER) {
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(BREVMAL_RIVER_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) = runBlocking {
        val brevkode = packet[BREVMAL_RIVER_KEY].asText().let { Brevkoder.valueOf(it) }
        opprettJournalfoerOgDistribuer(packet.sakId, brevkode, Systembruker.brev)
    }

    private suspend fun opprettJournalfoerOgDistribuer(
        sakId: Long,
        brevKode: Brevkoder,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        logger.info("Oppretter $brevKode-brev i sak $sakId")
        val brevOgData =
            retryOgPakkUt {
                brevoppretter.opprettBrev(
                    sakId = sakId,
                    behandlingId = null,
                    bruker = brukerTokenInfo,
                    brevKode = brevKode.redigering,
                    brevtype = brevKode.redigering.brevtype,
                )
            }
        logger.info("Ferdigstiller $brevKode-brev i sak $sakId")
        val brevId = brevOgData.first.id
        retryOgPakkUt {
            pdfGenerator.ferdigstillOgGenererPDF(
                id = brevId,
                bruker = brukerTokenInfo,
                automatiskMigreringRequest = null,
                avsenderRequest = { _, _ ->
                    AvsenderRequest(
                        saksbehandlerIdent = Fagsaksystem.EY.navn,
                        sakenhet = brevOgData.second.sak.enhet,
                        attestantIdent = Fagsaksystem.EY.navn,
                    )
                },
                brevKode = { _ -> brevKode },
            )
        }
        logger.info("Journalfører $brevKode-brev i sak $sakId")
        retryOgPakkUt { journalfoerBrevService.journalfoer(brevId, brukerTokenInfo) }
        logger.info("Distribuerer $brevKode-brev i sak $sakId")
        retryOgPakkUt { brevdistribuerer.distribuer(brevId) }
        logger.info("$brevKode-brev i sak $sakId ferdig distribuert")
    }
}
