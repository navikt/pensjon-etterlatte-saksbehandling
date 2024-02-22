package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.model.Slate
import no.nav.pensjon.brevbaker.api.model.RenderedJsonLetter

object BlockTilSlateKonverterer {
    internal fun konverter(it: RenderedJsonLetter) =
        Slate(
            it
                .blocks
                .flatMap { block -> tilSlateElement(block) }
                .toList(),
        )

    private fun tilSlateElement(block: RenderedJsonLetter.Block) =
        when (block.type) {
            RenderedJsonLetter.Block.Type.TITLE1 ->
                listOf(
                    Slate.Element(
                        type = Slate.ElementType.HEADING_TWO,
                        children = (block as RenderedJsonLetter.Block.Title1).content.map { konverterLiteralOgVariable(it) },
                    ),
                )

            RenderedJsonLetter.Block.Type.TITLE2 ->
                listOf(
                    Slate.Element(
                        type = Slate.ElementType.HEADING_THREE,
                        children = (block as RenderedJsonLetter.Block.Title2).content.map { konverterLiteralOgVariable(it) },
                    ),
                )

            // Hvis en paragraf fra brevbakeren inneholder lister, vil disse splittes ut og legges inn som en
            // Element-node i stedet for en InnerElement-node siden redigering av dette ikke støttes i slate-editoren.
            // De øvrige InnerElementene vil bli slått sammen og lagt til som egne Element-noder.
            RenderedJsonLetter.Block.Type.PARAGRAPH -> {
                val elements: MutableList<Slate.Element> = mutableListOf()
                val innerElements: MutableList<Slate.InnerElement> = mutableListOf()

                (block as RenderedJsonLetter.Block.Paragraph).content.map {
                    when (it.type) {
                        RenderedJsonLetter.ParagraphContent.Type.LITERAL, RenderedJsonLetter.ParagraphContent.Type.VARIABLE ->
                            konverterLiteralOgVariable(it).let { innerElement -> innerElements.add(innerElement) }

                        RenderedJsonLetter.ParagraphContent.Type.ITEM_LIST -> {
                            opprettElementFraInnerELementsOgNullstill(innerElements, elements)

                            Slate.Element(
                                type = Slate.ElementType.BULLETED_LIST,
                                children =
                                    (it as RenderedJsonLetter.ParagraphContent.ItemList).items
                                        .map { item -> konverterListItem(item) },
                            ).let { element -> elements.add(element) }
                        }
                    }
                }

                opprettElementFraInnerELementsOgNullstill(innerElements, elements)
                elements
            }
        }

    private fun opprettElementFraInnerELementsOgNullstill(
        innerElements: MutableList<Slate.InnerElement>,
        elements: MutableList<Slate.Element>,
    ) {
        if (innerElements.isNotEmpty()) {
            elements.add(
                Slate.Element(
                    type = Slate.ElementType.PARAGRAPH,
                    children = innerElements.toList(),
                ),
            )
            innerElements.clear()
        }
    }

    private fun konverterLiteralOgVariable(it: RenderedJsonLetter.ParagraphContent): Slate.InnerElement =
        Slate.InnerElement(
            type = Slate.ElementType.PARAGRAPH,
            text = (it as RenderedJsonLetter.ParagraphContent.Text).text,
        )

    private fun konverterListItem(it: RenderedJsonLetter.ParagraphContent.ItemList.Item): Slate.InnerElement =
        Slate.InnerElement(
            type = Slate.ElementType.LIST_ITEM,
            children =
                listOf(
                    Slate.InnerElement(
                        type = Slate.ElementType.PARAGRAPH,
                        text = it.content.joinToString { i -> i.text },
                    ),
                ),
        )
}
