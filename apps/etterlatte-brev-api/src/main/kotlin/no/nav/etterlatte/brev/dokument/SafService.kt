package no.nav.etterlatte.brev.dokument

import no.nav.etterlatte.brev.journalpost.BrukerIdType

interface SafService {
    suspend fun hentDokumenter(fnr: String, idType: BrukerIdType, accessToken: String): JournalpostResponse

    suspend fun hentDokumentPDF(
        journalpostId: String,
        dokumentInfoId: String,
        accessToken: String
    ): ByteArray
}