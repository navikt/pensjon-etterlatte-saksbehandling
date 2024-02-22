package no.nav.etterlatte.utbetaling.iverksetting.oppdrag

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.io.StringReader
import java.io.StringWriter
import javax.xml.stream.XMLInputFactory
import javax.xml.transform.stream.StreamSource

object OppdragJaxb {
    private val jaxbContext = JAXBContext.newInstance(Oppdrag::class.java)
    private val xmlInputFactory = XMLInputFactory.newInstance()

    fun toXml(oppdrag: Oppdrag): String {
        val marshaller = jaxbContext.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)

        val stringWriter = StringWriter()
        stringWriter.use {
            marshaller.marshal(oppdrag, stringWriter)
        }

        return stringWriter.toString()
    }

    fun toOppdrag(oppdragXml: String): Oppdrag {
        val oppdrag =
            jaxbContext.createUnmarshaller().unmarshal(
                xmlInputFactory.createXMLStreamReader(StreamSource(oppdragXml.toValidXml())),
                Oppdrag::class.java,
            )
        return oppdrag.value
    }

    private fun String.toValidXml() =
        this
            .replace("<oppdrag xmlns=", "<ns2:oppdrag xmlns:ns2=")
            .replace("</Oppdrag>", "</ns2:oppdrag>")
            .let { StringReader(it) }
}
