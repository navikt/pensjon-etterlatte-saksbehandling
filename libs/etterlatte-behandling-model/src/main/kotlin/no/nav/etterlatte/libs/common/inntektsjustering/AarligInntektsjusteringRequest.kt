package no.nav.etterlatte.libs.common.inntektsjustering

import no.nav.etterlatte.libs.common.sak.SakId
import java.time.Year
import java.time.YearMonth
import java.util.UUID

data class AarligInntektsjusteringRequest(
    val kjoering: String,
    val loependeFom: YearMonth = utledLoependeFom(),
    val saker: List<SakId>,
) {
    companion object {
        fun utledKjoering() = "INNTEKTSJUSTERING_JOBB_${Year.now().value}"

        fun utledLoependeFom() = YearMonth.of(Year.now().value, 1).plusYears(1)
    }
}

data class InntektsjusteringRequest(
    val sak: SakId,
    val journalpostId: String,
    val inntektsjusteringId: UUID,
    val inntekt: Int,
    val inntektUtland: Int,
) {
    companion object {
        fun utledKjoering(id: UUID) = "INNTEKTSJUSTERING_${utledLoependeFom()}_$id"

        fun utledLoependeFom() = YearMonth.of(Year.now().value, 1).plusYears(1)
    }
}
