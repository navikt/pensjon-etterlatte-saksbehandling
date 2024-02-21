package no.nav.etterlatte.brev.model

data class InnholdMedVedlegg(
    val innhold: () -> List<Slate.Element>,
    val innholdVedlegg: () -> List<BrevInnholdVedlegg>,
) {
    fun finnVedlegg(key: BrevVedleggKey): List<Slate.Element> = innholdVedlegg().find { vedlegg -> vedlegg.key == key }?.payload!!.elements
}
