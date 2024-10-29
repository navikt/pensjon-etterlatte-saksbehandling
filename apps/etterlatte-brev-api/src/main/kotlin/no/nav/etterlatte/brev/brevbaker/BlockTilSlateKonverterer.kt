package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.Slate
import no.nav.pensjon.brevbaker.api.model.LetterMarkup

object BlockTilSlateKonverterer {
    internal fun konverter(it: LetterMarkup) =
        Slate(
            it
                .blocks
                .flatMap { block -> tilSlateElement(block) }
                .toList(),
        )

    private fun tilSlateElement(block: LetterMarkup.Block) =
        when (block.type) {
            LetterMarkup.Block.Type.TITLE1 ->
                listOf(
                    Slate.Element(
                        type = Slate.ElementType.HEADING_TWO,
                        children = (block as LetterMarkup.Block.Title1).content.map { konverterLiteralOgVariable(it) },
                    ),
                )

            LetterMarkup.Block.Type.TITLE2 ->
                listOf(
                    Slate.Element(
                        type = Slate.ElementType.HEADING_THREE,
                        children = (block as LetterMarkup.Block.Title2).content.map { konverterLiteralOgVariable(it) },
                    ),
                )

            // Hvis en paragraf fra brevbakeren inneholder lister, vil disse splittes ut og legges inn som en
            // Element-node i stedet for en InnerElement-node siden redigering av dette ikke støttes i slate-editoren.
            // De øvrige InnerElementene vil bli slått sammen og lagt til som egne Element-noder.
            LetterMarkup.Block.Type.PARAGRAPH -> {
                val elements: MutableList<Slate.Element> = mutableListOf()
                val innerElements: MutableList<Slate.InnerElement> = mutableListOf()

                (block as LetterMarkup.Block.Paragraph).content.map {
                    when (it.type) {
                        LetterMarkup.ParagraphContent.Type.LITERAL,
                        LetterMarkup.ParagraphContent.Type.VARIABLE,
                        LetterMarkup.ParagraphContent.Type.NEW_LINE,
                        ->
                            konverterLiteralOgVariable(it).let { innerElement -> innerElements.add(innerElement) }

                        LetterMarkup.ParagraphContent.Type.ITEM_LIST -> {
                            opprettElementFraInnerELementsOgNullstill(innerElements, elements)

                            Slate
                                .Element(
                                    type = Slate.ElementType.BULLETED_LIST,
                                    children =
                                        (it as LetterMarkup.ParagraphContent.ItemList)
                                            .items
                                            .map { item -> konverterListItem(item) },
                                ).let { element -> elements.add(element) }
                        }
                        else -> {
                            throw IllegalArgumentException("Ukjent type: ${it.type}")
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

    private fun konverterLiteralOgVariable(it: LetterMarkup.ParagraphContent): Slate.InnerElement =
        Slate.InnerElement(
            type = Slate.ElementType.PARAGRAPH,
            text = (it as LetterMarkup.ParagraphContent.Text).text,
        )

    private fun konverterListItem(it: LetterMarkup.ParagraphContent.ItemList.Item): Slate.InnerElement =
        Slate.InnerElement(
            type = Slate.ElementType.LIST_ITEM,
            children =
                listOf(
                    Slate.InnerElement(
                        type = Slate.ElementType.PARAGRAPH,
                        text = it.content.joinToString("") { i -> i.text },
                    ),
                ),
        )
}
