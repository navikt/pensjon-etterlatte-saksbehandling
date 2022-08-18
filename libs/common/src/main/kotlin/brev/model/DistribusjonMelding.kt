package no.nav.etterlatte.libs.common.brev.model

import no.nav.etterlatte.libs.common.distribusjon.DistribusjonsType
import no.nav.etterlatte.libs.common.journalpost.Bruker

data class DistribusjonMelding(
    val behandlingId: String,
    val distribusjonType: DistribusjonsType,
    val brevId: Long,
    val mottaker: Mottaker,
    val mottakerAdresse: Adresse? = null,
    val bruker: Bruker,
    val tittel: String,
    val brevKode: String,
    val journalfoerendeEnhet: String
)

enum class BrevEventTypes {
    FERDIGSTILT, JOURNALFOERT, DISTRIBUERT;

    override fun toString(): String {
        return "BREV:$name"
    }
}