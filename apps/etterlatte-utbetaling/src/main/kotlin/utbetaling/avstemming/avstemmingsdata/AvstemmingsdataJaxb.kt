package no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBElement
import jakarta.xml.bind.Marshaller
import no.nav.virksomhet.tjenester.avstemming.informasjon.konsistensavstemmingsdata.v1.Konsistensavstemmingsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import java.io.StringWriter
import javax.xml.namespace.QName

object GrensesnittavstemmingsdataJaxb {
    private val jaxbContext = JAXBContext.newInstance(Avstemmingsdata::class.java)

    fun toXml(avstemmingsdata: Avstemmingsdata): String {
        val stringWriter = StringWriter()
        jaxbContext.createMarshaller().apply {
            setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        }.marshal(
            JAXBElement(QName("", "Avstemmingsdata"), Avstemmingsdata::class.java, avstemmingsdata),
            stringWriter
        )
        return stringWriter.toString()
    }
}

object KonsistensavstemmingsdataJaxb {
    private val jaxbContext = JAXBContext.newInstance(Konsistensavstemmingsdata::class.java)

    fun toXml(avstemmingsdata: Konsistensavstemmingsdata): String {
        val stringWriter = StringWriter()
        jaxbContext.createMarshaller().apply {
            setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        }.marshal(
            JAXBElement(QName("", "Konsistensavstemmingsdata"), Konsistensavstemmingsdata::class.java, avstemmingsdata),
            stringWriter
        )
        return stringWriter.toString()
    }
}