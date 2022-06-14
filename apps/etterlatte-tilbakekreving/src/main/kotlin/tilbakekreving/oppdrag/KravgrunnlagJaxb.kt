package no.nav.etterlatte.tilbakekreving.oppdrag

import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import javax.xml.bind.JAXBContext
import javax.xml.stream.XMLInputFactory
import javax.xml.transform.stream.StreamSource

object KravgrunnlagJaxb {
    private val jaxbContext = JAXBContext.newInstance(DetaljertKravgrunnlagDto::class.java)
    private val xmlInputFactory = XMLInputFactory.newInstance()

    fun toKravgrunnlag(kravgrunnlagXml: String): DetaljertKravgrunnlagDto {
        val kravgrunnlag = jaxbContext.createUnmarshaller().unmarshal(
            xmlInputFactory.createXMLStreamReader(StreamSource(kravgrunnlagXml)),
            DetaljertKravgrunnlagDto::class.java
        )
        return kravgrunnlag.value
    }
}
