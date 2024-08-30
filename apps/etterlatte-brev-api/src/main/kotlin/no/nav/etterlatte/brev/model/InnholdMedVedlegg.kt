package no.nav.etterlatte.brev.model

import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException

class ManglerPayloadForVedleggInnholdBrev(
    override val detail: String,
) : InternfeilException(detail)

data class InnholdMedVedlegg(
    val innhold: () -> List<Slate.Element>,
    val innholdVedlegg: () -> List<BrevInnholdVedlegg>,
) {
    fun finnVedlegg(key: BrevVedleggKey): List<Slate.Element> =
        innholdVedlegg().find { vedlegg -> vedlegg.key == key }?.payload?.elements
            ?: throw ManglerPayloadForVedleggInnholdBrev("Fant ikke vedlegg for brev av typen $key")
}
