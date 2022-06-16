package no.nav.etterlatte.tilbakekreving.oppdrag

import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import java.io.StringReader
import javax.xml.bind.JAXBContext
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.stream.XMLInputFactory

object KravgrunnlagJaxb {
    private val jaxbContext = JAXBContext.newInstance(DetaljertKravgrunnlagMelding::class.java)
    private val xmlInputFactory = XMLInputFactory.newInstance()

    fun toKravgrunnlag(kravgrunnlagXml: String): DetaljertKravgrunnlagDto {
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