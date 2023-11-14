package no.nav.etterlatte.trygdetid.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.trygdetid.KodeverkService
import no.nav.etterlatte.trygdetid.TrygdetidBeregningService
import no.nav.etterlatte.trygdetid.TrygdetidRepository
import no.nav.etterlatte.trygdetid.TrygdetidServiceImpl
import no.nav.etterlatte.trygdetid.avtale.AvtaleRepository
import no.nav.etterlatte.trygdetid.avtale.AvtaleService
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import no.nav.etterlatte.trygdetid.klienter.GrunnlagKlient
import no.nav.etterlatte.trygdetid.klienter.KodeverkKlient
import no.nav.etterlatte.trygdetid.klienter.VilkaarsvuderingKlient

class ApplicationContext {
    val config: Config = ConfigFactory.load()
    val properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv())
    val dataSource =
        DataSourceBuilder.createDataSource(
            jdbcUrl = properties.jdbcUrl,
            username = properties.dbUsername,
            password = properties.dbPassword,
        )
    private val grunnlagKlient = GrunnlagKlient(config, httpClient())
    private val vilkaarsvurderingKlient = VilkaarsvuderingKlient(config, httpClient())
    val behandlingKlient = BehandlingKlient(config, httpClient())
    val kodeverkService = KodeverkService(KodeverkKlient(config, httpClient()))
    val trygdetidService =
        TrygdetidServiceImpl(
            TrygdetidRepository(dataSource),
            behandlingKlient = behandlingKlient,
            grunnlagKlient = grunnlagKlient,
            vilkaarsvurderingKlient = vilkaarsvurderingKlient,
            beregnTrygdetidService = TrygdetidBeregningService,
            featureToggleService = FeatureToggleService.initialiser(featureToggleProperties(config)),
        )
    private val avtaleRepository = AvtaleRepository(dataSource)
    val avtaleService = AvtaleService(avtaleRepository)
}

private fun featureToggleProperties(config: Config) =
    FeatureToggleProperties(
        applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
        host = config.getString("funksjonsbrytere.unleash.host"),
        apiKey = config.getString("funksjonsbrytere.unleash.token"),
    )
