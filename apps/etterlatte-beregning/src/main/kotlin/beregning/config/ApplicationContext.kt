package no.nav.etterlatte.beregning.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.beregning.BeregningRepositoryImpl
import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.beregning.klienter.BehandlingKlientImpl
import no.nav.etterlatte.beregning.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.beregning.klienter.VilkaarsvurderingKlientImpl
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.ktor.httpClient

class ApplicationContext {
    private val config: Config = ConfigFactory.load()
    private val env = System.getenv()
    val properties: ApplicationProperties = ApplicationProperties.fromEnv(env)
    val dataSource = DataSourceBuilder.createDataSource(
        jdbcUrl = properties.jdbcUrl,
        username = properties.dbUsername,
        password = properties.dbPassword
    )
    val beregningService = BeregningService(
        beregningRepository = BeregningRepositoryImpl(dataSource),
        vilkaarsvurderingKlient = VilkaarsvurderingKlientImpl(config, httpClient()),
        grunnlagKlient = GrunnlagKlientImpl(config, httpClient()),
        behandlingKlient = BehandlingKlientImpl(config, httpClient())
    )
}