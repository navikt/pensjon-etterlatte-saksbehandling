package no.nav.etterlatte.dokument

import no.nav.etterlatte.libs.common.journalpost.BrukerIdType

interface JournalpostService {
    suspend fun hentDokumenter(fnr: String, idType: BrukerIdType, accessToken: String): JournalpostResponse

    suspend fun hentDokumentPDF(
        journalpostId: String,
        dokumentInfoId: String,
        accessToken: String
    ): ByteArray
}