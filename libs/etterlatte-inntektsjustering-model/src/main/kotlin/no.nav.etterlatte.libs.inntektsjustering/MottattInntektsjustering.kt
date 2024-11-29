package no.nav.etterlatte.libs.inntektsjustering

import no.nav.etterlatte.libs.common.sak.SakId
import java.time.YearMonth
import java.util.UUID

data class MottattInntektsjustering(
    val sak: SakId,
    val inntektsjusteringId: UUID,
    val journalpostId: String,
    val inntektsaar: Int,
    val arbeidsinntekt: Int,
    val naeringsinntekt: Int,
    val afpInntekt: Int?,
    val inntektFraUtland: Int,
    val datoForAaGaaAvMedAlderspensjon: YearMonth?,
) {
    companion object {
        fun utledKjoering(id: UUID) = "INNTEKTSJUSTERING_${utledLoependeFom()}_$id"

        // TODO må endres når vi mottar inntektsjusteringt inneværende år
        fun utledLoependeFom() = AarligInntektsjusteringRequest.utledLoependeFom()
    }
}
