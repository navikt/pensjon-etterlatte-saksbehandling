package no.nav.etterlatte.tilbakekreving.vedtak

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakResponse
import java.io.StringWriter

object TilbakekrevingJaxb {
    private val jaxbContext = JAXBContext.newInstance(TilbakekrevingsvedtakResponse::class.java)

    fun toXml(response: TilbakekrevingsvedtakResponse): String {
        val marshaller = jaxbContext.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)

        val stringWriter = StringWriter()
        stringWriter.use {
            marshaller.marshal(response, stringWriter)
        }

        return stringWriter.toString()
    }
}
