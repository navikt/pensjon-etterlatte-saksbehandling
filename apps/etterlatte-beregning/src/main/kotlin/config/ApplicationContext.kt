package no.nav.etterlatte.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.avkorting.AvkortingService
import no.nav.etterlatte.beregning.BeregnBarnepensjonService
import no.nav.etterlatte.beregning.BeregnOmstillingsstoenadService
import no.nav.etterlatte.beregning.BeregningRepository
import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagRepository
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.klienter.BehandlingKlientImpl
import no.nav.etterlatte.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.klienter.TrygdetidKlient
import no.nav.etterlatte.klienter.VilkaarsvurderingKlientImpl
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.ytelseMedGrunnlag.YtelseMedGrunnlagService
import java.net.URI

private fun featureToggleProperties(config: Config) = FeatureToggleProperties(
    applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
    uri = URI(config.getString("funksjonsbrytere.unleash.uri")),
    cluster = config.getString("funksjonsbrytere.unleash.cluster")
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
        beregningsGrunnlagService = beregningsGrunnlagService,
        grunnlagKlient = grunnlagKlient,
        trygdetidKlient = trygdetidKlient
    )
    val beregningRepository = BeregningRepository(dataSource)
    val beregningService = BeregningService(
        beregningRepository = beregningRepository,
        behandlingKlient = behandlingKlient,
        beregnBarnepensjonService = beregnBarnepensjonService,
        beregnOmstillingsstoenadService = beregnOmstillingsstoenadService,
        beregningsGrunnlagService = beregningsGrunnlagService
    )
    val avkortingRepository = AvkortingRepository(dataSource)
    val avkortingService = AvkortingService(
        behandlingKlient = behandlingKlient,
        avkortingRepository = avkortingRepository,
        beregningService = beregningService
    )
    val ytelseMedGrunnlagService = YtelseMedGrunnlagService(
        beregningRepository = beregningRepository,
        avkortingRepository = avkortingRepository
    )
}