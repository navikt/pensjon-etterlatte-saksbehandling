package no.nav.etterlatte.brev.distribusjon

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.distribusjon.BestillingsID
import no.nav.etterlatte.libs.common.distribusjon.DistribuerJournalpostRequest
import no.nav.etterlatte.libs.common.distribusjon.DistribusjonsTidspunktType
import no.nav.etterlatte.libs.common.distribusjon.DistribusjonsType
import no.nav.etterlatte.libs.common.toJson

interface DistribusjonService {
    fun distribuerJournalpost(
        brevId: Long,
        journalpostId: String,
        type: DistribusjonsType,
        tidspunkt: DistribusjonsTidspunktType,
        adresse: Adresse? = null
    ): BestillingsID
}

class DistribusjonServiceImpl(
    private val klient: DistribusjonKlient,
    private val db: BrevRepository
) : DistribusjonService {
    override fun distribuerJournalpost(
        brevId: Long,
        journalpostId: String,
        type: DistribusjonsType,
        tidspunkt: DistribusjonsTidspunktType,
        adresse: Adresse?
    ): BestillingsID = runBlocking {
        val request = DistribuerJournalpostRequest(
            journalpostId = journalpostId,
            bestillendeFagsystem = "EY",
            distribusjonstype = type,
            distribusjonstidspunkt = tidspunkt,
            dokumentProdApp = "etterlatte-brev-api"
        )

        klient.distribuerJournalpost(request)
            .also {
                db.setBestillingsId(brevId, it.bestillingsId)
                db.oppdaterStatus(brevId, Status.DISTRIBUERT, it.toJson())
            }
            .let { it.bestillingsId }
    }
}