package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.pensjon.brevbaker.api.model.LetterMarkup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class BlockTilSlateKonvertererTest {
    @Test
    fun `kan lese inn json fra brevbakeren`() {
        val originalJson = this.javaClass.getResource("/brevbaker/barnepensjon_vedtak_omregning.json")!!.readText()
        objectMapper.addMixIn(LetterMarkup.Block::class.java, BrevbakerJSONBlockMixIn::class.java)
        objectMapper.addMixIn(LetterMarkup.ParagraphContent::class.java, BrevbakerJSONParagraphMixIn::class.java)
        val letterMarkup = deserialize<LetterMarkup>(originalJson)

        val konvertert = BlockTilSlateKonverterer.konverter(letterMarkup)
        assertEquals(konvertert.elements.size, letterMarkup.blocks.size)
    }

    @Test
    fun `skal konvertere title1, title2 og paragraph til HEADING_TWO, HEADING_THREE og PARAGRAPH`() {
        val originalJson =
            this.javaClass
                .getResource("/brevbaker/brevbaker_payload_med_title1_title2_og_paragraf.json")!!
                .readText()
        objectMapper.addMixIn(LetterMarkup.Block::class.java, BrevbakerJSONBlockMixIn::class.java)
        objectMapper.addMixIn(LetterMarkup.ParagraphContent::class.java, BrevbakerJSONParagraphMixIn::class.java)
        val letterMarkup = deserialize<LetterMarkup>(originalJson)

        val konvertert = BlockTilSlateKonverterer.konverter(letterMarkup)

        assertEquals(Slate.ElementType.HEADING_TWO, konvertert.elements[0].type)
        assertEquals(Slate.ElementType.HEADING_THREE, konvertert.elements[1].type)
        assertEquals(Slate.ElementType.PARAGRAPH, konvertert.elements[2].type)
        assertEquals(konvertert.elements.size, 3)
    }

    @Test
    fun `skal konvertere item_list til BULLETED_LIST og flytte fra paragraph til toppnode`() {
        val originalJson = this.javaClass.getResource("/brevbaker/brevbaker_payload_med_item_list.json")!!.readText()
        objectMapper.addMixIn(LetterMarkup.Block::class.java, BrevbakerJSONBlockMixIn::class.java)
        objectMapper.addMixIn(LetterMarkup.ParagraphContent::class.java, BrevbakerJSONParagraphMixIn::class.java)
        val letterMarkup = deserialize<LetterMarkup>(originalJson)

        val konvertert = BlockTilSlateKonverterer.konverter(letterMarkup)

        assertEquals(Slate.ElementType.PARAGRAPH, konvertert.elements[0].type)
        assertEquals(Slate.ElementType.BULLETED_LIST, konvertert.elements[1].type)
        assertEquals(3, konvertert.elements[1].children.size)
        konvertert.elements[1].children.forEach {
            assertEquals(Slate.ElementType.LIST_ITEM, it.type)
            assertEquals(Slate.ElementType.PARAGRAPH, it.children?.first()?.type)
            assertNotNull(it.children?.first()?.text)
        }
        assertEquals(Slate.ElementType.PARAGRAPH, konvertert.elements[2].type)
        assertEquals(2, konvertert.elements[2].children.size)
        assertEquals(Slate.ElementType.PARAGRAPH, konvertert.elements[3].type)
        assertEquals(konvertert.elements.size, 4)
    }
}
