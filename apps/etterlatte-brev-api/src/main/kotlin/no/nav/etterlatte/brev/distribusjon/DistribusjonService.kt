package no.nav.etterlatte.brev.distribusjon

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.brev.distribusjon.Adresse as DistAdresse

interface DistribusjonService {
    fun distribuerJournalpost(
        brevId: Long,
        type: DistribusjonsType,
        mottaker: Mottaker,
        bruker: BrukerTokenInfo,
    ): DistribuerJournalpostResponse
}

internal class DistribusjonServiceImpl(
    private val klient: DistribusjonKlient,
) : DistribusjonService {
    override fun distribuerJournalpost(
        brevId: Long,
        type: DistribusjonsType,
        mottaker: Mottaker,
        bruker: BrukerTokenInfo,
    ): DistribuerJournalpostResponse =
        runBlocking {
            val request =
                DistribuerJournalpostRequest(
                    journalpostId = checkNotNull(mottaker.journalpostId) { "JournalpostID mangler på mottaker=${mottaker.id}" },
                    bestillendeFagsystem = Fagsaksystem.EY.navn,
                    distribusjonstype = type,
                    adresse =
                        DistAdresse(
                            AdresseType.fra(mottaker.adresse.adresseType),
                            mottaker.adresse.adresselinje1,
                            mottaker.adresse.adresselinje2,
                            mottaker.adresse.adresselinje3,
                            mottaker.adresse.postnummer,
                            mottaker.adresse.poststed,
                            mottaker.adresse.landkode,
                        ),
                    distribusjonstidspunkt = DistribusjonsTidspunktType.KJERNETID,
                    dokumentProdApp = "etterlatte-brev-api",
                    tvingSentralPrint = mottaker.tvingSentralPrint,
                )

            klient.distribuerJournalpost(request, bruker)
        }
}
