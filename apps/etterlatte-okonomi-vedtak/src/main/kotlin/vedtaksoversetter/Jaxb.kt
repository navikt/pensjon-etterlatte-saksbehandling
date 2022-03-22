package no.nav.etterlatte.vedtaksoversetter

import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.io.StringWriter
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller

fun Oppdrag.toXml(): String {
    val jaxbContext = JAXBContext.newInstance(Oppdrag::class.java)
    val marshaller = jaxbContext.createMarshaller()
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)

    val stringWriter = StringWriter()
    stringWriter.use {
        marshaller.marshal(this, stringWriter)
    }

    return stringWriter.toString()
}
