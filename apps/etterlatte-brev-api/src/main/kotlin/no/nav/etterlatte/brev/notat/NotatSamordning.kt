package no.nav.etterlatte.brev.notat

import no.nav.etterlatte.brev.NotatParametre
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.sak.SakId

class SamordningsnotatParametre(
    val sakId: SakId,
    val vedtakId: Long,
    val samordningsmeldingId: Long,
    val kommentar: String,
    val saksbehandlerId: String,
) : NotatParametre

fun opprettSamordningsnotatPayload(params: NotatParametre?): Slate {
    if (params !is SamordningsnotatParametre) {
        throw IllegalArgumentException("Samordningsnotat krever SamordningsnotatParametre")
    }
    return Slate(
        listOf(
            Slate.Element(
                Slate.ElementType.HEADING_TWO,
                listOf(Slate.InnerElement(Slate.ElementType.PARAGRAPH, "Samordning")),
            ),
            Slate.Element(
                Slate.ElementType.PARAGRAPH,
                listOf(Slate.InnerElement(Slate.ElementType.PARAGRAPH, params.kommentar)),
            ),
            Slate.Element(
                Slate.ElementType.HEADING_THREE,
                listOf(Slate.InnerElement(Slate.ElementType.PARAGRAPH, "Tekniske data")),
            ),
            Slate.Element(
                Slate.ElementType.BULLETED_LIST,
                listOf(
                    listeelement("Saksbehandler: ${params.saksbehandlerId}"),
                    listeelement("SaksID: ${params.sakId}"),
                    listeelement("VedtakID: ${params.vedtakId}"),
                    listeelement("SamordningsmeldingID: ${params.samordningsmeldingId}"),
                ),
            ),
        ),
    )
}

private fun listeelement(tekst: String) =
    Slate.InnerElement(
        type = Slate.ElementType.LIST_ITEM,
        children = listOf(Slate.InnerElement(Slate.ElementType.PARAGRAPH, tekst)),
    )
