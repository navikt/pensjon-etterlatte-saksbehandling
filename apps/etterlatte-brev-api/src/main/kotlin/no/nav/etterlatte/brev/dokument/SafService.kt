package no.nav.etterlatte.brev.dokument

import no.nav.etterlatte.brev.journalpost.BrukerIdType
import no.nav.etterlatte.token.Bruker

interface SafService {
    suspend fun hentDokumenter(fnr: String, idType: BrukerIdType, bruker: Bruker): HentJournalposterResult

    suspend fun hentDokumentPDF(
        journalpostId: String,
        dokumentInfoId: String,
        accessToken: String
    ): ByteArray
}