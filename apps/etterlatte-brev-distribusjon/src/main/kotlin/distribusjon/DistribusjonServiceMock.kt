package no.nav.etterlatte.distribusjon

import no.nav.etterlatte.libs.common.brev.model.Adresse
import no.nav.etterlatte.libs.common.distribusjon.BestillingsID
import no.nav.etterlatte.libs.common.distribusjon.DistribusjonsTidspunktType
import no.nav.etterlatte.libs.common.distribusjon.DistribusjonsType
import java.util.*

class DistribusjonServiceMock : DistribusjonService {
    override fun distribuerJournalpost(
        journalpostId: String,
        type: DistribusjonsType,
        tidspunkt: DistribusjonsTidspunktType,
        adresse: Adresse?
    ): BestillingsID {
        return UUID.randomUUID().toString()
    }
}