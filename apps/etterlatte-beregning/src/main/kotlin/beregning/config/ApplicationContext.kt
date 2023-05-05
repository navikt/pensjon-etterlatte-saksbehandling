package no.nav.etterlatte.beregning.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.beregning.AvkortingRepository
import no.nav.etterlatte.beregning.AvkortingService
import no.nav.etterlatte.beregning.BeregnBarnepensjonService
import no.nav.etterlatte.beregning.BeregnOmstillingsstoenadService
import no.nav.etterlatte.beregning.BeregningRepository
import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.beregning.InntektAvkortingService
import no.nav.etterlatte.beregning.grunnlag.BarnepensjonBeregningsGrunnlagMigreringJobb
import no.nav.etterlatte.beregning.klienter.BehandlingKlientImpl
import no.nav.etterlatte.beregning.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.beregning.klienter.TrygdetidKlient
import no.nav.etterlatte.beregning.klienter.VilkaarsvurderingKlientImpl
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleServiceProperties
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.libs.ktor.httpClient

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

    val vilkaarsvurderingKlient = VilkaarsvurderingKlientImpl(config, httpClient())
    val grunnlagKlient = GrunnlagKlientImpl(config, httpClient())
    val trygdetidKlient = TrygdetidKlient(config, httpClient())
    val behandlingKlient = BehandlingKlientImpl(config, httpClient())

    val beregnBarnepensjonService = BeregnBarnepensjonService(
        vilkaarsvurderingKlient = vilkaarsvurderingKlient,
        grunnlagKlient = grunnlagKlient
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
        beregningRepository = BeregningRepository(dataSource)
    )

    private val featureToggleService: FeatureToggleService = FeatureToggleService.initialiser(
        featureToggleProperties(config)
    )

    private val leaderElectionKlient = LeaderElection(env.getValue("ELECTOR_PATH"), httpClient())

    private val barnepensjonBeregningsGrunnlagMigreringJobb = BarnepensjonBeregningsGrunnlagMigreringJobb(
        leaderElectionKlient,
        dataSource,
        featureToggleService
    )

    init {
        barnepensjonBeregningsGrunnlagMigreringJobb.schedule()
    }
}