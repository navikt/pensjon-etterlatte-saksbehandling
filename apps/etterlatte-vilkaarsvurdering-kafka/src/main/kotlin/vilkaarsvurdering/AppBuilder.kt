package no.nav.etterlatte.vilkaarsvurdering

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.vilkaarsvurdering.services.VilkaarsvurderingServiceImpl
import rapidsandrivers.RapidsAndRiversAppBuilder

class AppBuilder(props: Map<String, String>) : RapidsAndRiversAppBuilder(props) {
    private val config: Config = ConfigFactory.load()
    private val vedtakHttpKlient = httpClientClientCredentials(
        azureAppClientId = config.getString("pdl.client_id"),
        azureAppJwk = config.getString("pdl.client_jwk"),
        azureAppWellKnownUrl = config.getString("pdl.well_known_url"),
        azureAppScope = config.getString("pdl.outbound")
    )
    private val vedtakUrl = requireNotNull(props["ETTERLATTE_VILKAARSVURDERING_URL"])

    fun lagVedtakKlient(): VilkaarsvurderingServiceImpl {
        return VilkaarsvurderingServiceImpl(vedtakHttpKlient, vedtakUrl)
    }
}