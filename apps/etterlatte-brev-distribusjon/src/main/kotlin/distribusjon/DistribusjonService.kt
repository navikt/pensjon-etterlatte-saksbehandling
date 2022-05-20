package no.nav.etterlatte.distribusjon

interface DistribusjonService {
    fun distribuerJournalpost(journalpostId: String): String
}
