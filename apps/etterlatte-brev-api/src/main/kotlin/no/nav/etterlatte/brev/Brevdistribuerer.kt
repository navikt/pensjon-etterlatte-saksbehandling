package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.distribusjon.DistribusjonService
import no.nav.etterlatte.brev.distribusjon.DistribusjonsTidspunktType
import no.nav.etterlatte.brev.distribusjon.DistribusjonsType
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.Status

class Brevdistribuerer(
    private val db: BrevRepository,
    private val distribusjonService: DistribusjonService,
) {
    fun distribuer(id: BrevID): String {
        val brev = db.hentBrev(id)

        if (brev.status != Status.JOURNALFOERT) {
            throw IllegalStateException(
                "Forventet status ${Status.JOURNALFOERT} på brev (id=${brev.id}), men fikk ${brev.status}",
            )
        }

        val journalpostId =
            requireNotNull(db.hentJournalpostId(id)) {
                "JournalpostID mangler på brev (id=${brev.id}, status=${brev.status})"
            }

        val mottaker =
            requireNotNull(brev.mottaker) {
                "Mottaker må være satt for å kunne distribuere brevet (id: $id)"
            }

        return distribusjonService.distribuerJournalpost(
            brevId = brev.id,
            journalpostId = journalpostId,
            type = DistribusjonsType.ANNET,
            tidspunkt = DistribusjonsTidspunktType.KJERNETID,
            adresse = mottaker.adresse,
        )
    }
}
