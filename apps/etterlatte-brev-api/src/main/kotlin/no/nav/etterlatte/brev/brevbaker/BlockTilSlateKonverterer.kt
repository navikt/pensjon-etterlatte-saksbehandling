package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.model.Slate
import no.nav.pensjon.brevbaker.api.model.RenderedLetterMarkdown

object BlockTilSlateKonverterer {
    internal fun konverter(it: RenderedLetterMarkdown) =
        Slate(
            it
                .blocks
                .flatMap { block -> tilSlateElement(block) }
                .toList(),
        )

    private fun tilSlateElement(block: RenderedLetterMarkdown.Block) =
        when (block.type) {
            RenderedLetterMarkdown.Block.Type.TITLE1 ->
                listOf(
                    Slate.Element(
                        type = Slate.ElementType.HEADING_TWO,
                        children = (block as RenderedLetterMarkdown.Block.Title1).content.map { konverterLiteralOgVariable(it) },
                    ),
                )

            RenderedLetterMarkdown.Block.Type.TITLE2 ->
                listOf(
                    Slate.Element(
                        type = Slate.ElementType.HEADING_THREE,
                        children = (block as RenderedLetterMarkdown.Block.Title2).content.map { konverterLiteralOgVariable(it) },
                    ),
                )

            // Hvis en paragraf fra brevbakeren inneholder lister, vil disse splittes ut og legges inn som en
            // Element-node i stedet for en InnerElement-node siden redigering av dette ikke støttes i slate-editoren.
            // De øvrige InnerElementene vil bli slått sammen og lagt til som egne Element-noder.
            RenderedLetterMarkdown.Block.Type.PARAGRAPH -> {
                val elements: MutableList<Slate.Element> = mutableListOf()
                val innerElements: MutableList<Slate.InnerElement> = mutableListOf()

                (block as RenderedLetterMarkdown.Block.Paragraph).content.map {
                    when (it.type) {
                        RenderedLetterMarkdown.ParagraphContent.Type.LITERAL, RenderedLetterMarkdown.ParagraphContent.Type.VARIABLE ->
                            konverterLiteralOgVariable(it).let { innerElement -> innerElements.add(innerElement) }

                        RenderedLetterMarkdown.ParagraphContent.Type.ITEM_LIST -> {
                            opprettElementFraInnerELementsOgNullstill(innerElements, elements)

                            Slate.Element(
                                type = Slate.ElementType.BULLETED_LIST,
                                children =
                                    (it as RenderedLetterMarkdown.ParagraphContent.ItemList).items
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

    private fun konverterLiteralOgVariable(it: RenderedLetterMarkdown.ParagraphContent): Slate.InnerElement =
        Slate.InnerElement(
            type = Slate.ElementType.PARAGRAPH,
            text = (it as RenderedLetterMarkdown.ParagraphContent.Text).text,
        )

    private fun konverterListItem(it: RenderedLetterMarkdown.ParagraphContent.ItemList.Item): Slate.InnerElement =
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
