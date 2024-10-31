package no.nav.etterlatte.brev.distribusjon

import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import org.slf4j.LoggerFactory
import java.util.UUID

class Brevdistribuerer(
    private val db: BrevRepository,
    private val distribusjonService: DistribusjonService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun distribuer(
        brevId: BrevID,
        distribusjonsType: DistribusjonsType = DistribusjonsType.ANNET,
    ): List<BestillingsID> {
        logger.info("Starter distribuering av brev $brevId.")

        val brev = db.hentBrev(brevId)

        if (brev.status != Status.JOURNALFOERT) {
            throw FeilStatusForDistribusjon(brev.id, brev.status)
        } else if (brev.mottakere.isEmpty()) {
            throw DistribusjonError("Det må finnes minst 1 mottaker for å kunne distribuere brevet (brevId: $brevId)")
        } else if (brev.mottakere.size > 2) {
            throw DistribusjonError("Distribusjon av brev med flere enn 2 mottakere er ikke støttet (brevId: $brevId)")
        }

        return brev.mottakere
            .filter { it.bestillingId.isNullOrBlank() }
            .map { mottaker -> sendTilMottaker(brevId, mottaker, distribusjonsType) }
            .also { db.settBrevDistribuert(brevId, it) }
            .map { it.bestillingsId }
    }

    private fun sendTilMottaker(
        brevId: BrevID,
        mottaker: Mottaker,
        distribusjonsType: DistribusjonsType,
    ): DistribuerJournalpostResponse {
        if (mottaker.journalpostId == null) {
            throw JournalpostIdMangler(brevId, mottaker.id)
        }

        return distribusjonService
            .distribuerJournalpost(
                brevId = brevId,
                type = distribusjonsType,
                mottaker = mottaker,
            ).also {
                db.lagreBestillingId(mottaker.id, it)

                logger.info("Brev $brevId ble distribuert til mottaker=${mottaker.id} med bestillingId=${it.bestillingsId}")
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

class JournalpostIdMangler(
    brevId: BrevID,
    mottakerId: UUID,
) : UgyldigForespoerselException(
        code = "KAN_IKKE_DISTRIBUERE_UTEN_JOURNALPOST_ID",
        detail = "JournalpostID mangler på mottaker=$mottakerId (brevId: $brevId)",
    )

class DistribusjonError(
    detail: String,
) : UgyldigForespoerselException(
        code = "FEIL_VED_DISTRIBUSJON",
        detail = detail,
    )
