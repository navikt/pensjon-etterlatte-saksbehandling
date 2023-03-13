package no.nav.etterlatte.beregning.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.beregning.BeregnBarnepensjonService
import no.nav.etterlatte.beregning.BeregnOmstillingsstoenadService
import no.nav.etterlatte.beregning.BeregningRepository
import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.beregning.klienter.BehandlingKlientImpl
import no.nav.etterlatte.beregning.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.beregning.klienter.VilkaarsvurderingKlientImpl
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.ktor.httpClient

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
    val beregnBarnepensjonService = BeregnBarnepensjonService(
        vilkaarsvurderingKlient = vilkaarsvurderingKlient,
        grunnlagKlient = grunnlagKlient
    )
    val beregnOmstillingsstoenadService = BeregnOmstillingsstoenadService(
        vilkaarsvurderingKlient = vilkaarsvurderingKlient,
        grunnlagKlient = grunnlagKlient
    )
    val behandlingKlient = BehandlingKlientImpl(config, httpClient())
    val beregningService = BeregningService(
        beregningRepository = BeregningRepository(dataSource),
        behandlingKlient = behandlingKlient,
        beregnBarnepensjonService = beregnBarnepensjonService,
        beregnOmstillingsstoenadService = beregnOmstillingsstoenadService
    )
}