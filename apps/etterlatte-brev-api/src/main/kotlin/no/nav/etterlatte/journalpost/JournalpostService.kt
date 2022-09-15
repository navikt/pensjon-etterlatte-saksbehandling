package no.nav.etterlatte.journalpost

import no.nav.etterlatte.libs.common.journalpost.BrukerIdType

interface JournalpostService {
    suspend fun hentInnkommendeBrev(fnr: String, idType: BrukerIdType, accessToken: String): JournalpostResponse

    suspend fun hentInnkommendeBrevInnhold(
        journalpostId: String,
        dokumentInfoId: String,
        accessToken: String
    ): ByteArray
}