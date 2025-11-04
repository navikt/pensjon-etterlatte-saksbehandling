package no.nav.etterlatte.trygdetid.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.database.ApplicationProperties
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.trygdetid.SjekkAvvikJobb
import no.nav.etterlatte.trygdetid.SjekkAvvikService
import no.nav.etterlatte.trygdetid.TrygdetidAvvikRepository
import no.nav.etterlatte.trygdetid.TrygdetidBeregningService
import no.nav.etterlatte.trygdetid.TrygdetidRepository
import no.nav.etterlatte.trygdetid.TrygdetidServiceImpl
import no.nav.etterlatte.trygdetid.avtale.AvtaleRepository
import no.nav.etterlatte.trygdetid.avtale.AvtaleService
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import no.nav.etterlatte.trygdetid.klienter.GrunnlagKlient
import no.nav.etterlatte.trygdetid.klienter.PesysKlientImpl
import no.nav.etterlatte.trygdetid.klienter.VedtaksvurderingKlientImpl
import java.time.Duration
import java.time.temporal.ChronoUnit

class ApplicationContext {
    val config: Config = ConfigFactory.load()
    val properties: ApplicationProperties = ApplicationProperties.fromEnv(Miljoevariabler.systemEnv())
    val dataSource =
        DataSourceBuilder.createDataSource(
            jdbcUrl = properties.jdbcUrl,
            username = properties.dbUsername,
            password = properties.dbPassword,
        )
    private val grunnlagKlient = GrunnlagKlient(config, httpClient())
    private val avtaleRepository = AvtaleRepository(dataSource)

    val behandlingKlient = BehandlingKlient(config, httpClient())
    val vedtaksvurderingKlient = VedtaksvurderingKlientImpl(config, httpClient())
    val avtaleService = AvtaleService(avtaleRepository)

    val featureToggleService = FeatureToggleService.initialiser(featureToggleProperties(config))
    private val trygdetidRepository = TrygdetidRepository(dataSource)

    val trygdetidService =
        TrygdetidServiceImpl(
            trygdetidRepository = trygdetidRepository,
            behandlingKlient = behandlingKlient,
            grunnlagKlient = grunnlagKlient,
            beregnTrygdetidService = TrygdetidBeregningService,
            pesysKlient = PesysKlientImpl(config, httpClient()),
            avtaleService = avtaleService,
            vedtaksvurderingKlient = vedtaksvurderingKlient,
            featureToggleService = featureToggleService,
        )

    private val leaderElection = LeaderElection(config.getString("ELECTOR_PATH"))
    private val avvikRepository = TrygdetidAvvikRepository(dataSource)
    private val sjekkAvvikService =
        SjekkAvvikService(
            avvikRepository = avvikRepository,
            trygdetidRepository = trygdetidRepository,
            avtaleRepository = avtaleRepository,
        )
    val sjekkAvvikJobb =
        SjekkAvvikJobb(
            sjekkAvvikService = sjekkAvvikService,
            leaderElection = leaderElection,
            featureToggleService = featureToggleService,
            periode = Duration.of(10, ChronoUnit.MINUTES),
            initialDelaySeconds = 120,
        )
}

private fun featureToggleProperties(config: Config) =
    FeatureToggleProperties(
        applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
        host = config.getString("funksjonsbrytere.unleash.host"),
        apiKey = config.getString("funksjonsbrytere.unleash.token"),
    )
