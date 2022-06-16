package no.nav.etterlatte.tilbakekreving.oppdrag

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.tilbakekreving.domene.KravgrunnlagRootDto
import no.nav.etterlatte.tilbakekreving.domene.TilbakekrevingsmeldingDto

class TilbakekrevingsmeldingMapper {

    private val xmlMapper = XmlMapper(JacksonXmlModule().apply { setDefaultUseWrapper(false) }).apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    fun toTilbakekreving(xml: String): TilbakekrevingsmeldingDto =
        xmlMapper.readValue<KravgrunnlagRootDto>(xml)

}