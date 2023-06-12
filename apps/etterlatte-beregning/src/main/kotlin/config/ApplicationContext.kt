package no.nav.etterlatte.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.avkorting.AvkortingService
import no.nav.etterlatte.avkorting.InntektAvkortingService
import no.nav.etterlatte.beregning.BeregnBarnepensjonService
import no.nav.etterlatte.beregning.BeregnOmstillingsstoenadService
import no.nav.etterlatte.beregning.BeregningRepository
import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagRepository
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleServiceProperties
import no.nav.etterlatte.klienter.BehandlingKlientImpl
import no.nav.etterlatte.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.klienter.TrygdetidKlient
import no.nav.etterlatte.klienter.VilkaarsvurderingKlientImpl
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.ytelseMedGrunnlag.YtelseMedGrunnlagService

private fun featureToggleProperties(config: Config) = mapOf(
    FeatureToggleServiceProperties.ENABLED.navn to config.getString("funksjonsbrytere.enabled"),
    FeatureToggleServiceProperties.APPLICATIONNAME.navn to config.getString(
        "funksjonsbrytere.unleash.applicationName"
    ),
    FeatureToggleServiceProperties.URI.navn to config.getString("funksjonsbrytere.unleash.uri"),
    FeatureToggleServiceProperties.CLUSTER.navn to config.getString("funksjonsbrytere.unleash.cluster")
)

class ApplicationContext {
    val config: Config = ConfigFactory.load()
    private val env = System.getenv()
    val properties: ApplicationProperties = ApplicationProperties.fromEnv(env)
    val dataSource = DataSourceBuilder.createDataSource(
        jdbcUrl = properties.jdbcUrl,
        username = properties.dbUsername,
        password = properties.dbPassword
    )

    val featureToggleService: FeatureToggleService = FeatureToggleService.initialiser(featureToggleProperties(config))

    val vilkaarsvurderingKlient = VilkaarsvurderingKlientImpl(config, httpClient())
    val grunnlagKlient = GrunnlagKlientImpl(config, httpClient())
    val trygdetidKlient = TrygdetidKlient(config, httpClient())
    val behandlingKlient = BehandlingKlientImpl(config, httpClient())

    private val beregningsGrunnlagRepository = BeregningsGrunnlagRepository(dataSource)
    val beregningsGrunnlagService = BeregningsGrunnlagService(
        beregningsGrunnlagRepository = beregningsGrunnlagRepository,
        behandlingKlient = behandlingKlient
    )

    val beregnBarnepensjonService = BeregnBarnepensjonService(
        grunnlagKlient = grunnlagKlient,
        vilkaarsvurderingKlient = vilkaarsvurderingKlient,
        beregningsGrunnlagService = beregningsGrunnlagService,
        trygdetidKlient = trygdetidKlient,
        featureToggleService = featureToggleService
    )
    val beregnOmstillingsstoenadService = BeregnOmstillingsstoenadService(
        vilkaarsvurderingKlient = vilkaarsvurderingKlient,
        grunnlagKlient = grunnlagKlient,
        trygdetidKlient = trygdetidKlient
    )
    val beregningService = BeregningService(
        beregningRepository = BeregningRepository(dataSource),
        behandlingKlient = behandlingKlient,
        beregnBarnepensjonService = beregnBarnepensjonService,
        beregnOmstillingsstoenadService = beregnOmstillingsstoenadService
    )
    val avkortingService = AvkortingService(
        behandlingKlient = behandlingKlient,
        inntektAvkortingService = InntektAvkortingService,
        avkortingRepository = AvkortingRepository(dataSource),
        beregningService = beregningService
    )
    val ytelseMedGrunnlagService = YtelseMedGrunnlagService(
        avkortingRepository = AvkortingRepository(dataSource)
    )
}