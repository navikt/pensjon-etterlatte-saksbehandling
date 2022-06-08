package no.nav.etterlatte.distribusjon

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.brev.model.Adresse
import no.nav.etterlatte.libs.common.distribusjon.BestillingID
import no.nav.etterlatte.libs.common.distribusjon.DistribuerJournalpostRequest
import no.nav.etterlatte.libs.common.distribusjon.DistribusjonsTidspunktType
import no.nav.etterlatte.libs.common.distribusjon.DistribusjonsType

interface DistribusjonService {
    fun distribuerJournalpost(
        journalpostId: String,
        type: DistribusjonsType,
        tidspunkt: DistribusjonsTidspunktType,
        adresse: Adresse? = null
    ): BestillingID
}

class DistribusjonServiceImpl(private val klient: DistribusjonKlient) : DistribusjonService {
    override fun distribuerJournalpost(
        journalpostId: String,
        type: DistribusjonsType,
        tidspunkt: DistribusjonsTidspunktType,
        adresse: Adresse?
    ): BestillingID = runBlocking {
        val request = DistribuerJournalpostRequest(
            journalpostId = journalpostId,
            bestillendeFagsystem = "EY", // todo: felles fagsystemkode
            distribusjonstype = type,
            distribusjonstidspunkt = tidspunkt,
            dokumentProdApp = "etterlate-brev-api"
        )

        klient.distribuerJournalpost(request).bestillingId
    }
}
