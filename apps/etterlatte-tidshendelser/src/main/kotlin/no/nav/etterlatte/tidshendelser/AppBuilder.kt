package no.nav.etterlatte.tidshendelser

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.tidshendelser.klient.GrunnlagKlient

class AppBuilder(env: Miljoevariabler) {
    private val config: Config = ConfigFactory.load()

    val dataSource = DataSourceBuilder.createDataSource(env.props)

    private val grunnlagHttpClient: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = env.requireEnvValue("AZURE_APP_CLIENT_ID"),
            azureAppJwk = env.requireEnvValue("AZURE_APP_JWK"),
            azureAppWellKnownUrl = env.requireEnvValue("AZURE_APP_WELL_KNOWN_URL"),
            azureAppScope = env.requireEnvValue("ETTERLATTE_GRUNNLAG_AZURE_SCOPE"),
        )
    }

    val grunnlagKlient =
        GrunnlagKlient(
            grunnlagHttpClient,
            config.getString("etterlatte.grunnlag.url"),
        )

    private val hendelseDao = HendelseDao(dataSource)

    val aldersovergangerService =
        AldersovergangerService(
            hendelseDao,
            grunnlagKlient,
        )
}
