package no.nav.etterlatte.brev.distribusjon

import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import org.slf4j.LoggerFactory

class Brevdistribuerer(
    private val db: BrevRepository,
    private val distribusjonService: DistribusjonService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun distribuer(
        brevId: BrevID,
        distribusjonsType: DistribusjonsType = DistribusjonsType.ANNET,
        journalpostIdInn: String? = null,
    ): BestillingsID {
        logger.info("Starter distribuering av brev $brevId.")

        val brev = db.hentBrev(brevId)

        if (brev.status != Status.JOURNALFOERT) {
            throw FeilStatusForDistribusjon(brev.id, brev.status)
        }

        val journalpostId =
            journalpostIdInn
                ?: requireNotNull(db.hentJournalpostId(brevId)) {
                    "JournalpostID mangler på brev (id=${brev.id}, status=${brev.status})"
                }

        check(brev.mottakere.isNotEmpty()) {
            "Det må finnes minst 1 mottaker for å kunne distribuere brevet (id: $brevId)"
        }

        /*
         * TODO EY-3627:
         *  Må håndtere flere [BestillingsID] når det blir mulig å legge til flere mottakere.
         */
        return with(brev.mottakere.single()) {
            distribusjonService
                .distribuerJournalpost(
                    brevId = brev.id,
                    journalpostId = journalpostId,
                    type = distribusjonsType,
                    tidspunkt = DistribusjonsTidspunktType.KJERNETID,
                    adresse = adresse,
                    tvingSentralPrint = tvingSentralPrint,
                ).also { logger.info("Distribuerte brev $brevId") }
        }
    }
}

class FeilStatusForDistribusjon(
    brevID: BrevID,
    status: Status,
) : UgyldigForespoerselException(
        code = "FEIL_STATUS_FOR_DISTRIBUSJON",
        detail = "Kan ikke distribuere brev med status ${status.name.lowercase()}",
        meta =
            mapOf(
                "brevId" to brevID,
                "status" to status,
            ),
    )
