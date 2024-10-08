package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import org.slf4j.LoggerFactory

class ManglerPayloadForVedleggInnholdBrev(
    override val detail: String,
) : InternfeilException(detail)

private val logger = LoggerFactory.getLogger(InnholdMedVedlegg::class.java)

data class InnholdMedVedlegg(
    val innhold: () -> List<Slate.Element>,
    val innholdVedlegg: () -> List<BrevInnholdVedlegg>,
) {
    fun finnVedlegg(key: BrevVedleggKey): List<Slate.Element> {
        val vedlegg = innholdVedlegg().find { vedlegg -> vedlegg.key == key }?.payload?.elements
        if (vedlegg == null) {
            logger.warn(
                "Fant ikke vedlegg for brev av type=$key, lette " +
                    "etter ${innholdVedlegg().joinToString { it.key.name }}",
            )
            throw ManglerPayloadForVedleggInnholdBrev(
                "Fant ikke vedlegg som hører til brevet. Brevet må tilbakestilles for å " +
                    "få med seg riktig vedlegg og tekst. Hvis det ikke går, meld sak i porten.",
            )
        }
        return vedlegg
    }
}
