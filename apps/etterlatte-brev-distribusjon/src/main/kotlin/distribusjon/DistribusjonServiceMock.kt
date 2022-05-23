package no.nav.etterlatte.distribusjon

import no.nav.etterlatte.libs.common.person.Adresse
import java.util.*


class DistribusjonServiceMock : DistribusjonService {
    override fun distribuerJournalpost(
        journalpostId: String,
        type: DistribusjonsType,
        tidspunkt: DistribusjonsTidspunktType,
        adresse: Adresse?
    ): BestillingID {
        return UUID.randomUUID().toString()
    }
}
