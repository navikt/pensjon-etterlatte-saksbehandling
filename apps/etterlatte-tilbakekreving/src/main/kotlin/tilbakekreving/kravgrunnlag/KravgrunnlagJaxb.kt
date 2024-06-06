package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.status.v1.EndringKravOgVedtakstatus
import no.nav.tilbakekreving.status.v1.KravOgVedtakstatus
import java.io.StringReader
import javax.xml.stream.XMLInputFactory

object KravgrunnlagJaxb {
    private val jaxbContextKravgrunnlag = JAXBContext.newInstance(DetaljertKravgrunnlagMelding::class.java)
    private val jaxbContextKravOgVedtakstatus = JAXBContext.newInstance(EndringKravOgVedtakstatus::class.java)
    private val xmlInputFactory = XMLInputFactory.newInstance()

    fun toDetaljertKravgrunnlagDto(xml: String): DetaljertKravgrunnlagDto {
        val kravgrunnlag =
            jaxbContextKravgrunnlag.createUnmarshaller().unmarshal(
                xmlInputFactory.createXMLStreamReader(StringReader(xml)),
                DetaljertKravgrunnlagMelding::class.java,
            )
        return kravgrunnlag.value.detaljertKravgrunnlag!!
    }

    fun toKravOgVedtakstatus(xml: String): KravOgVedtakstatus {
        val endringKravOgVedtakstatus =
            jaxbContextKravOgVedtakstatus.createUnmarshaller().unmarshal(
                xmlInputFactory.createXMLStreamReader(StringReader(xml)),
                EndringKravOgVedtakstatus::class.java,
            )
        return endringKravOgVedtakstatus.value.kravOgVedtakstatus
    }
}

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "detaljertKravgrunnlagMelding")
data class DetaljertKravgrunnlagMelding(
    @field:XmlElement(required = true, namespace = "urn:no:nav:tilbakekreving:kravgrunnlag:detalj:v1")
    val detaljertKravgrunnlag: DetaljertKravgrunnlagDto? = null,
)
