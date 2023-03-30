package no.nav.etterlatte.vilkaarsvurdering

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.vilkaarsvurdering.services.VilkaarsvurderingServiceImpl

class AppBuilder(props: Miljoevariabler) {
    private val config: Config = ConfigFactory.load()
    private val vilkaarsvurderingHttpKlient = httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("vilkaarsvurdering.azure.scope")
    )
    private val vilkaarsvurderingUrl = requireNotNull(props["ETTERLATTE_VILKAARSVURDERING_URL"])

    fun lagVilkaarsvurderingKlient(): VilkaarsvurderingServiceImpl {
        return VilkaarsvurderingServiceImpl(vilkaarsvurderingHttpKlient, vilkaarsvurderingUrl)
    }
}