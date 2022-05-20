package no.nav.etterlatte.distribusjon

import java.util.*


class DistribusjonServiceMock : DistribusjonService {
    override fun distribuerJournalpost(journalpostId: String): BestillingID {
        return UUID.randomUUID().toString()
    }
}

typealias BestillingID = String
