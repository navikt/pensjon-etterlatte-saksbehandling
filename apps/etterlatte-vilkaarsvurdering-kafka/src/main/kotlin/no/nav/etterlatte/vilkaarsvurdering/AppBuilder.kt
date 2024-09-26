package no.nav.etterlatte.vilkaarsvurdering

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.vilkaarsvurdering.services.VilkaarsvurderingServiceImpl

class AppBuilder {
    private val config: Config = ConfigFactory.load()
    private val vilkaarsvurderingHttpKlient =
        httpClientClientCredentials(
            azureAppClientId = config.getString("azure.app.client.id"),
            azureAppJwk = config.getString("azure.app.jwk"),
            azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
            azureAppScope = config.getString("behandling.azure.scope"),
        )
    private val vilkaarsvurderingUrl = requireNotNull(config.getString("behandling.resource.url"))

    fun lagVilkaarsvurderingKlient(): VilkaarsvurderingServiceImpl =
        VilkaarsvurderingServiceImpl(vilkaarsvurderingHttpKlient, vilkaarsvurderingUrl)
}
