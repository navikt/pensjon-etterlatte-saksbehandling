package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.distribusjon.BestillingsID
import no.nav.etterlatte.brev.distribusjon.DistribusjonService
import no.nav.etterlatte.brev.distribusjon.DistribusjonsTidspunktType
import no.nav.etterlatte.brev.distribusjon.DistribusjonsType
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.Status
import org.slf4j.LoggerFactory

class Brevdistribuerer(
    private val db: BrevRepository,
    private val distribusjonService: DistribusjonService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

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

    fun distribuerVedtaksbrev(
        brevId: Long,
        distribusjonType: DistribusjonsType,
        journalpostId: String,
    ): BestillingsID {
        logger.info("Starter distribuering av brev.")

        val brev = db.hentBrev(brevId)

        val mottaker =
            requireNotNull(brev.mottaker) {
                "Kan ikke distribuere brev når mottaker er 'null' i brev med id=${brev.id}"
            }

        val bestillingsId =
            distribusjonService.distribuerJournalpost(
                brevId = brev.id,
                journalpostId = journalpostId,
                type = distribusjonType,
                tidspunkt = DistribusjonsTidspunktType.KJERNETID,
                adresse = mottaker.adresse,
            )
        return bestillingsId
    }
}
