package no.nav.etterlatte.rivers

import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.JournalfoerBrevService
import no.nav.etterlatte.brev.PDFGenerator
import no.nav.etterlatte.brev.adresse.AvsenderRequest
import no.nav.etterlatte.brev.distribusjon.Brevdistribuerer
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.ManueltBrevMedTittelData
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import org.slf4j.LoggerFactory

class FerdigstillJournalfoerOgDistribuerBrev(
    private val pdfGenerator: PDFGenerator,
    private val journalfoerBrevService: JournalfoerBrevService,
    private val brevdistribuerer: Brevdistribuerer,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun ferdigstillOgGenererPDF(
        brevKode: Brevkoder,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: String,
        brevId: BrevID,
    ): BrevID {
        logger.info("Ferdigstiller $brevKode-brev i sak $sakId")
        retryOgPakkUt {
            pdfGenerator.ferdigstillOgGenererPDF(
                id = brevId,
                bruker = brukerTokenInfo,
                avsenderRequest = { _, _, _ ->
                    AvsenderRequest(
                        saksbehandlerIdent = Fagsaksystem.EY.navn,
                        sakenhet = enhet,
                        attestantIdent = Fagsaksystem.EY.navn,
                    )
                },
                brevKodeMapping = { brevKode },
                brevDataMapping = { ManueltBrevMedTittelData(it.innholdMedVedlegg.innhold(), it.tittel) },
            )
        }
        return brevId
    }

    suspend fun journalfoerOgDistribuer(
        brevKode: Brevkoder,
        sakId: SakId,
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
