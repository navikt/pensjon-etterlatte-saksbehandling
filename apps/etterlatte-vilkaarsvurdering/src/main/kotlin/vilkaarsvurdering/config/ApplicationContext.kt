package no.nav.etterlatte.vilkaarsvurdering.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepository
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.etterlatte.vilkaarsvurdering.klienter.BehandlingKlientImpl
import no.nav.etterlatte.vilkaarsvurdering.klienter.GrunnlagKlientImpl

class ApplicationContext {
    val config: Config = ConfigFactory.load()
    val properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv())
    val dataSource = DataSourceBuilder.createDataSource(
        jdbcUrl = properties.jdbcUrl,
        username = properties.dbUsername,
        password = properties.dbPassword
    )
    val behandlingKlient = BehandlingKlientImpl(config, httpClient())
    val vilkaarsvurderingService = VilkaarsvurderingService(
        vilkaarsvurderingRepository = VilkaarsvurderingRepository(dataSource),
        behandlingKlient = behandlingKlient,
        grunnlagKlient = GrunnlagKlientImpl(config, httpClient())
    )
}