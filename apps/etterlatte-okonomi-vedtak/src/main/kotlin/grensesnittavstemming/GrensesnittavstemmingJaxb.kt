package no.nav.etterlatte.grensesnittavstemming

import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import java.io.StringWriter
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBElement
import javax.xml.bind.Marshaller
import javax.xml.namespace.QName

object GrensesnittavstemmingJaxb {
    private val jaxbContext = JAXBContext.newInstance(Avstemmingsdata::class.java)

    fun toXml(avstemmingsmelding: Avstemmingsdata): String {
        val stringWriter = StringWriter()
        jaxbContext.createMarshaller().apply {
            setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        }.marshal(
            JAXBElement(QName("", "Avstemmingsdata"), Avstemmingsdata::class.java, avstemmingsmelding),
            stringWriter
        )
        return stringWriter.toString()
    }

}