package no.nav.etterlatte.brev.distribusjon

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.brev.distribusjon.Adresse as DistAdresse

interface DistribusjonService {
    fun distribuerJournalpost(
        brevId: Long,
        journalpostId: String,
        type: DistribusjonsType,
        tidspunkt: DistribusjonsTidspunktType,
        adresse: Adresse,
        tvingSentralPrint: Boolean,
    ): BestillingsID
}

internal class DistribusjonServiceImpl(
    private val klient: DistribusjonKlient,
    private val db: BrevRepository,
) : DistribusjonService {
    override fun distribuerJournalpost(
        brevId: Long,
        journalpostId: String,
        type: DistribusjonsType,
        tidspunkt: DistribusjonsTidspunktType,
        adresse: Adresse,
        tvingSentralPrint: Boolean,
    ): BestillingsID =
        runBlocking {
            val request =
                DistribuerJournalpostRequest(
                    journalpostId = journalpostId,
                    bestillendeFagsystem = Fagsaksystem.EY.navn,
                    distribusjonstype = type,
                    adresse =
                        DistAdresse(
                            AdresseType.fra(adresse.adresseType),
                            adresse.adresselinje1,
                            adresse.adresselinje2,
                            adresse.adresselinje3,
                            adresse.postnummer,
                            adresse.poststed,
                            adresse.landkode,
                        ),
                    distribusjonstidspunkt = tidspunkt,
                    dokumentProdApp = "etterlatte-brev-api",
                    tvingSentralPrint = tvingSentralPrint,
                )

            klient
                .distribuerJournalpost(request)
                .also { db.settBrevDistribuert(brevId, it) }
                .bestillingsId
        }
}
