package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import java.io.StringReader
import javax.xml.stream.XMLInputFactory

object KravgrunnlagJaxb {
    private val jaxbContext = JAXBContext.newInstance(DetaljertKravgrunnlagMelding::class.java)
    private val xmlInputFactory = XMLInputFactory.newInstance()

    fun toDetaljertKravgrunnlagDto(kravgrunnlagXml: String): DetaljertKravgrunnlagDto {
        val kravgrunnlag = jaxbContext.createUnmarshaller().unmarshal(
            xmlInputFactory.createXMLStreamReader(StringReader(kravgrunnlagXml)),
            DetaljertKravgrunnlagMelding::class.java
        )
        return kravgrunnlag.value.detaljertKravgrunnlag!!
    }
}

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "detaljertKravgrunnlagMelding")
data class DetaljertKravgrunnlagMelding(
    @field:XmlElement(required = true, namespace = "urn:no:nav:tilbakekreving:kravgrunnlag:detalj:v1")
    val detaljertKravgrunnlag: DetaljertKravgrunnlagDto? = null
)