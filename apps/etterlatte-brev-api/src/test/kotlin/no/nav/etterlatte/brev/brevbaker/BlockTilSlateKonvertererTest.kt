package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.pensjon.brevbaker.api.model.RenderedJsonLetter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BlockTilSlateKonvertererTest {
    @Test
    fun `kan lese inn json fra brevbakeren`() {
        val originalJson = this.javaClass.getResource("/barnepensjon_vedtak_omregning.json")!!.readText()
        objectMapper.addMixIn(RenderedJsonLetter.Block::class.java, BrevbakerJSONBlockMixIn::class.java)
        objectMapper.addMixIn(RenderedJsonLetter.ParagraphContent::class.java, BrevbakerJSONParagraphMixIn::class.java)
        val renderedJsonLetter = deserialize<RenderedJsonLetter>(originalJson)

        val konvertert = BlockTilSlateKonverterer.konverter(renderedJsonLetter)
        assertEquals(konvertert.elements.size, renderedJsonLetter.blocks.size)
    }

    @Test
    fun `skal ta punktlister i json fra brevbakeren ut og opprette egne slate-elementer`() {
        val originalJson = this.javaClass.getResource("/omstillingsstoenad_vedtak_med_liste.json")!!.readText()
        objectMapper.addMixIn(RenderedJsonLetter.Block::class.java, BrevbakerJSONBlockMixIn::class.java)
        objectMapper.addMixIn(RenderedJsonLetter.ParagraphContent::class.java, BrevbakerJSONParagraphMixIn::class.java)
        val renderedJsonLetter = deserialize<RenderedJsonLetter>(originalJson)

        val konvertert = BlockTilSlateKonverterer.konverter(renderedJsonLetter)

        assertEquals(Slate.ElementType.PARAGRAPH, konvertert.elements[0].type)
        assertEquals(Slate.ElementType.BULLETED_LIST, konvertert.elements[1].type)
        assertEquals(3, konvertert.elements[1].children.size)
        assertEquals(Slate.ElementType.PARAGRAPH, konvertert.elements[2].type)
        assertEquals(2, konvertert.elements[2].children.size)
        assertEquals(Slate.ElementType.PARAGRAPH, konvertert.elements[3].type)
        assertEquals(konvertert.elements.size, 4)
    }
}
