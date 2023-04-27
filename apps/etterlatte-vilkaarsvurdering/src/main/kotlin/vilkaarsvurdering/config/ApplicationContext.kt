package no.nav.etterlatte.vilkaarsvurdering.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.vilkaarsvurdering.DelvilkaarRepository
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepository
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.etterlatte.vilkaarsvurdering.klienter.BehandlingKlientImpl
import no.nav.etterlatte.vilkaarsvurdering.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.vilkaarsvurdering.migrering.MigreringRepository
import no.nav.etterlatte.vilkaarsvurdering.migrering.MigreringService

class ApplicationContext {
    val config: Config = ConfigFactory.load()
    val properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv())
    val dataSource = DataSourceBuilder.createDataSource(
        jdbcUrl = properties.jdbcUrl,
        username = properties.dbUsername,
        password = properties.dbPassword
    )
    val behandlingKlient = BehandlingKlientImpl(config, httpClient())
    private val delvilkaarRepository = DelvilkaarRepository()
    private val vilkaarsvurderingRepository = VilkaarsvurderingRepository(dataSource, delvilkaarRepository)
    val vilkaarsvurderingService = VilkaarsvurderingService(
        vilkaarsvurderingRepository = vilkaarsvurderingRepository,
        behandlingKlient = behandlingKlient,
        grunnlagKlient = GrunnlagKlientImpl(config, httpClient())
    )
    val migreringService = MigreringService(
        MigreringRepository(delvilkaarRepository, dataSource),
        vilkaarsvurderingRepository
    )
}