package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.brevbaker.BrevbakerJSONResponse
import no.nav.etterlatte.brev.model.Slate

object BlockTilSlateKonverterer {
    fun konverter(it: BrevbakerJSONResponse) = Slate(
        it
            .blocks
            .map { block -> tilSlateElement(block) }
            .toList()
    )

    private fun tilSlateElement(block: BrevbakerJSONResponse.Block) = when (block.type) {
        BrevbakerJSONResponse.Block.Type.TITLE1 -> Slate.Element(
            type = Slate.ElementType.HEADING_TWO,
            children = children(block)
        )

        BrevbakerJSONResponse.Block.Type.TITLE2 -> Slate.Element(
            type = Slate.ElementType.HEADING_THREE,
            children = children(block)
        )

        BrevbakerJSONResponse.Block.Type.PARAGRAPH -> Slate.Element(
            type = Slate.ElementType.PARAGRAPH,
            children = children(block)
        )
    }

    private fun children(block: BrevbakerJSONResponse.Block): List<Slate.InnerElement> = when (block.type) {
        BrevbakerJSONResponse.Block.Type.TITLE1 -> (block as BrevbakerJSONResponse.Block.Title1).content.map {
            konverter(
                it
            )
        }

        BrevbakerJSONResponse.Block.Type.TITLE2 -> (block as BrevbakerJSONResponse.Block.Title2).content.map {
            konverter(
                it
            )
        }

        BrevbakerJSONResponse.Block.Type.PARAGRAPH -> (block as BrevbakerJSONResponse.Block.Paragraph).content.map {
            konverter(
                it
            )
        }
    }

    private fun konverter(it: BrevbakerJSONResponse.ParagraphContent): Slate.InnerElement = when (it.type) {
        BrevbakerJSONResponse.ParagraphContent.Type.ITEM_LIST -> Slate.InnerElement(
            type = Slate.ElementType.BULLETED_LIST,
            children =
            (it as BrevbakerJSONResponse.ParagraphContent.ItemList).items
                .map { item ->
                    Slate.InnerElement(
                        type = Slate.ElementType.LIST_ITEM,
                        text = item.content.joinToString()
                    )
                }
        )

        BrevbakerJSONResponse.ParagraphContent.Type.LITERAL, BrevbakerJSONResponse.ParagraphContent.Type.VARIABLE ->
            Slate.InnerElement(
                type = Slate.ElementType.PARAGRAPH,
                text = (it as BrevbakerJSONResponse.ParagraphContent.Text).text
            )
    }
}