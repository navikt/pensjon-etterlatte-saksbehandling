package no.nav.etterlatte.brev.dokument

import no.nav.etterlatte.brev.dokarkiv.BrukerIdType
import no.nav.etterlatte.token.BrukerTokenInfo

interface SafService {
    suspend fun hentDokumenter(
        fnr: String,
        idType: BrukerIdType,
        brukerTokenInfo: BrukerTokenInfo,
    ): HentDokumentoversiktBrukerResult

    suspend fun hentDokumentPDF(
        journalpostId: String,
        dokumentInfoId: String,
        accessToken: String,
    ): ByteArray

    suspend fun hentJournalpost(
        journalpostId: String,
        brukerTokenInfo: BrukerTokenInfo,
    ): HentJournalpostResult
}
