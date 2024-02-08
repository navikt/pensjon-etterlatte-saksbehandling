package no.nav.etterlatte.rivers

import no.nav.etterlatte.brev.Brevoppretter
import no.nav.etterlatte.brev.JournalfoerBrevService
import no.nav.etterlatte.brev.PDFGenerator
import no.nav.etterlatte.brev.adresse.AvsenderRequest
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.Brevkoder
import no.nav.etterlatte.brev.distribusjon.Brevdistribuerer
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.ManueltBrevData
import no.nav.etterlatte.brev.model.ManueltBrevMedTittelData
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Fagsaksystem
import org.slf4j.LoggerFactory
import java.util.UUID

class OpprettFerdigstillJournalfoerOgDistribuerBrev(
    private val brevoppretter: Brevoppretter,
    private val pdfGenerator: PDFGenerator,
    private val journalfoerBrevService: JournalfoerBrevService,
    private val brevdistribuerer: Brevdistribuerer,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun opprettJournalfoerOgDistribuer(
        sakId: Long,
        brevKode: Brevkoder,
        brukerTokenInfo: BrukerTokenInfo,
        behandlingId: UUID?,
    ) {
        val brevOgData = opprett(brevKode, sakId, behandlingId, brukerTokenInfo)
        val brevId = ferdigstillOgGenererPDF(brevKode, sakId, brevOgData, brukerTokenInfo)
        journalfoerOgDistribuer(brevKode, sakId, brevId, brukerTokenInfo)
    }

    suspend fun opprett(
        brevKode: Brevkoder,
        sakId: Long,
        behandlingId: UUID?,
        brukerTokenInfo: BrukerTokenInfo,
    ): Pair<Brev, GenerellBrevData> {
        logger.info("Oppretter $brevKode-brev i sak $sakId")
        val brevOgData =
            retryOgPakkUt {
                brevoppretter.opprettBrev(
                    sakId = sakId,
                    behandlingId = behandlingId,
                    bruker = brukerTokenInfo,
                    brevKode = { brevKode.redigering },
                    brevtype = brevKode.redigering.brevtype,
                ) { ManueltBrevData() }
            }
        return brevOgData
    }

    suspend fun ferdigstillOgGenererPDF(
        brevKode: Brevkoder,
        sakId: Long,
        brevOgData: Pair<Brev, GenerellBrevData>,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevID {
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
                brevKode = { brevKode },
                brevData = { ManueltBrevMedTittelData(it.innholdMedVedlegg.innhold(), it.tittel) },
            )
        }
        return brevId
    }

    suspend fun journalfoerOgDistribuer(
        brevKode: Brevkoder,
        sakId: Long,
        brevId: BrevID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        logger.info("Journalfører $brevKode-brev i sak $sakId")
        retryOgPakkUt { journalfoerBrevService.journalfoer(brevId, brukerTokenInfo) }
        logger.info("Distribuerer $brevKode-brev i sak $sakId")
        retryOgPakkUt { brevdistribuerer.distribuer(brevId) }
        logger.info("$brevKode-brev i sak $sakId ferdig distribuert")
    }
}
