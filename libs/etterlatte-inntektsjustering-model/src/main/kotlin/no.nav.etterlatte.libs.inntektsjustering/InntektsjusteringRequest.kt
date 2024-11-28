package no.nav.etterlatte.libs.inntektsjustering

import no.nav.etterlatte.libs.common.sak.SakId
import java.util.UUID

data class InntektsjusteringRequest(
    val sak: SakId,
    val journalpostId: String,
    val inntektsjusteringId: UUID,
    val inntekt: Int,
    val inntektUtland: Int,
) {
    companion object {
        fun utledKjoering(id: UUID) = "INNTEKTSJUSTERING_${utledLoependeFom()}_$id"

        // TODO må endres når vi mottar inntektsjusteringt inneværende år
        fun utledLoependeFom() = AarligInntektsjusteringRequest.utledLoependeFom()
    }
}
