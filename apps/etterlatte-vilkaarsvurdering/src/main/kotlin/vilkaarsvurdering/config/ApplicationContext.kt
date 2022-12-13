package no.nav.etterlatte.vilkaarsvurdering.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepositoryImpl
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.etterlatte.vilkaarsvurdering.behandling.BehandlingKlientImpl
import no.nav.etterlatte.vilkaarsvurdering.grunnlag.GrunnlagKlientImpl

class ApplicationContext {
    private val properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv())
    private val config: Config = ConfigFactory.load()
    val dataSourceBuilder = DataSourceBuilder(properties.jdbcUrl, properties.dbUsername, properties.dbPassword)
    val vilkaarsvurderingService = VilkaarsvurderingService(
        vilkaarsvurderingRepository = VilkaarsvurderingRepositoryImpl(dataSourceBuilder.dataSource()),
        behandlingKlient = BehandlingKlientImpl(config, httpClient()),
        grunnlagKlient = GrunnlagKlientImpl(config, httpClient())
    )
}