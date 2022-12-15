package no.nav.etterlatte.vilkaarsvurdering.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepository
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.etterlatte.vilkaarsvurdering.behandling.BehandlingKlientImpl
import no.nav.etterlatte.vilkaarsvurdering.grunnlag.GrunnlagKlientImpl

class ApplicationContext {
    private val config: Config = ConfigFactory.load()
    val properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv())
    val dataSource = DataSourceBuilder.createDataSource(
        properties.jdbcUrl,
        properties.dbUsername,
        properties.dbPassword
    )
    val vilkaarsvurderingService = VilkaarsvurderingService(
        vilkaarsvurderingRepository = VilkaarsvurderingRepository(dataSource),
        behandlingKlient = BehandlingKlientImpl(config, httpClient()),
        grunnlagKlient = GrunnlagKlientImpl(config, httpClient())
    )
}