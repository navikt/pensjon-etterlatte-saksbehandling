package no.nav.etterlatte.tilbakekreving.vedtak

import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingPortType
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.slf4j.LoggerFactory
import javax.xml.namespace.QName

class TilbakekrevingKlientConfig(private val tilbakekrevingServiceUrl: String) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun tilbakekrevingPortType(): TilbakekrevingPortType {
        logger.info("Bruker tilbakekrevingService url $tilbakekrevingServiceUrl")

        // TODO må man ha inn proxy mot STS her?
        return JaxWsProxyFactoryBean().apply {
            address = tilbakekrevingServiceUrl
            wsdlURL = WSDL
            serviceName = SERVICE
            endpointName = PORT
            serviceClass = TilbakekrevingPortType::class.java
            // features = listOf(LoggingFeature())
        }.create() as TilbakekrevingPortType
    }

    private companion object {
        private const val WSDL = "wsdl/no/nav/tilbakekreving/tilbakekreving-v1-tjenestespesifikasjon.wsdl"
        private const val NAMESPACE = "http://okonomi.nav.no/tilbakekrevingService/"
        private val SERVICE = QName(NAMESPACE, "TilbakekrevingService")
        private val PORT = QName(NAMESPACE, "TilbakekrevingServicePort")
    }
}
