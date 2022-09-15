package no.nav.etterlatte.journalpost

import no.nav.etterlatte.libs.common.journalpost.BrukerIdType
import java.io.FileNotFoundException

class JournalpostServiceMock : JournalpostService {
    override suspend fun hentInnkommendeBrevInnhold(
        journalpostId: String,
        dokumentInfoId: String,
        accessToken: String
    ): ByteArray =
        readFile("/pdf/innkommende.pdf")

    override suspend fun hentInnkommendeBrev(
        fnr: String,
        idType: BrukerIdType,
        accessToken: String
    ): JournalpostResponse =
        JournalpostResponse(
            DokumentoversiktBruker(
                Journalposter(
                    listOf(
                        Journalpost(
                            journalpostId = "524975615",
                            tittel = "Kravblankett",
                            journalposttype = "I",
                            journalstatus = "JOURNALFOERT",
                            dokumenter = listOf(
                                Dokumenter(
                                    dokumentInfoId = "549168554",
                                    tittel = "Kravblankett",
                                    dokumentvarianter = listOf(
                                        Dokumentvarianter(
                                            saksbehandlerHarTilgang = true
                                        )
                                    )
                                )
                            ),
                            avsenderMottaker = AvsenderMottaker(
                                id = "29018322402",
                                navn = "KOPP GRØNN",
                                erLikBruker = true
                            ),
                            kanal = "NAV_NO",
                            datoOpprettet = "2022-02-17T10:09:01"
                        )
                    )
                )
            )
        )

    fun readFile(file: String) = JournalpostServiceMock::class.java.getResource(file)?.readBytes()
        ?: throw FileNotFoundException("Fant ikke filen $file")
}