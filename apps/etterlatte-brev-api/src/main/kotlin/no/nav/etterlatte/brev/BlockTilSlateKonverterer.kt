package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.model.Slate
import no.nav.pensjon.brevbaker.api.model.RenderedJsonLetter

object BlockTilSlateKonverterer {
    fun konverter(it: RenderedJsonLetter) = Slate(
        it
            .blocks
            .map { block -> tilSlateElement(block) }
            .toList()
    )

    private fun tilSlateElement(block: RenderedJsonLetter.Block) = when (block.type) {
        RenderedJsonLetter.Block.Type.TITLE1 -> TODO(
            "Stopper konvertering til slate siden vi fikk Title1 i innhold, som ikke skal skje"
        )
        RenderedJsonLetter.Block.Type.TITLE2 -> Slate.Element(
            type = Slate.ElementType.HEADING_TWO,
            children = children(block)
        )

        RenderedJsonLetter.Block.Type.PARAGRAPH -> Slate.Element(
            type = Slate.ElementType.PARAGRAPH,
            children = children(block)
        )
    }

    private fun children(block: RenderedJsonLetter.Block): List<Slate.InnerElement> = when (block.type) {
        RenderedJsonLetter.Block.Type.TITLE1 -> TODO(
            "Stopper konvertering til slate siden vi fikk Title1 i innhold, som ikke skal skje"
        )
        RenderedJsonLetter.Block.Type.TITLE2 -> (block as RenderedJsonLetter.Block.Title2).content.map { konverter(it) }
        RenderedJsonLetter.Block.Type.PARAGRAPH -> (block as RenderedJsonLetter.Block.Paragraph).content.map {
            konverter(
                it
            )
        }
    }

    private fun konverter(it: RenderedJsonLetter.ParagraphContent): Slate.InnerElement = when (it.type) {
        RenderedJsonLetter.ParagraphContent.Type.ITEM_LIST -> Slate.InnerElement(
            type = Slate.ElementType.BULLETED_LIST,
            children =
            (it as RenderedJsonLetter.ParagraphContent.ItemList).items
                .map { item ->
                    Slate.InnerElement(
                        type = Slate.ElementType.LIST_ITEM,
                        text = item.content.joinToString()
                    )
                }
        )

        RenderedJsonLetter.ParagraphContent.Type.LITERAL, RenderedJsonLetter.ParagraphContent.Type.VARIABLE ->
            Slate.InnerElement(
                type = Slate.ElementType.PARAGRAPH,
                text = (it as RenderedJsonLetter.ParagraphContent.Text).text
            )
    }
}