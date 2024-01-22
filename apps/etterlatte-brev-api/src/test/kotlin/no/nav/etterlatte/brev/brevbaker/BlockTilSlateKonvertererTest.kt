package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.pensjon.brevbaker.api.model.RenderedJsonLetter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BlockTilSlateKonvertererTest {
    @Test
    fun `kan lese inn json fra brevbakeren`() {
        val originalJson = this.javaClass.getResource("/barnepensjon_vedtak_omregning.json")!!.readText()
        objectMapper.addMixIn(RenderedJsonLetter.Block::class.java, BrevbakerJSONBlockMixIn::class.java)
        objectMapper.addMixIn(RenderedJsonLetter.ParagraphContent::class.java, BrevbakerJSONParagraphMixIn::class.java)
        val renderedJsonLetter = deserialize<RenderedJsonLetter>(originalJson)

        val konvertert = BlockTilSlateKonverterer.konverter(renderedJsonLetter)
        Assertions.assertEquals(konvertert.elements.size, renderedJsonLetter.blocks.size)
    }

    @Test
    fun `kan lese inn json fra brevbakeren og splitte ut eventuelle lister under paragraf til topp-noder i Slate`() {
        val originalJson = this.javaClass.getResource("/omstillingsstoenad_vedtak_med_liste.json")!!.readText()
        objectMapper.addMixIn(RenderedJsonLetter.Block::class.java, BrevbakerJSONBlockMixIn::class.java)
        objectMapper.addMixIn(RenderedJsonLetter.ParagraphContent::class.java, BrevbakerJSONParagraphMixIn::class.java)
        val renderedJsonLetter = deserialize<RenderedJsonLetter>(originalJson)

        val konvertert = BlockTilSlateKonverterer.konverter(renderedJsonLetter)
        Assertions.assertEquals(konvertert.elements.size, 2)
    }
}
