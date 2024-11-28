package no.nav.etterlatte.libs.common.inntektsjustering

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

        fun utledLoependeFom() = AarligInntektsjusteringRequest.utledLoependeFom()
    }
}
