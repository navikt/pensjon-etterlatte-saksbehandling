package no.nav.etterlatte.vilkaarsvurdering.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.database.ApplicationProperties
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.libs.ktor.AppConfig.ELECTOR_PATH
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.no.nav.etterlatte.vilkaarsvurdering.MigrertYrkesskadeJob
import no.nav.etterlatte.no.nav.etterlatte.vilkaarsvurdering.MigrertYrkesskadeOppdaterer
import no.nav.etterlatte.vilkaarsvurdering.AldersovergangService
import no.nav.etterlatte.vilkaarsvurdering.DelvilkaarRepository
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepository
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.etterlatte.vilkaarsvurdering.klienter.BehandlingKlientImpl
import no.nav.etterlatte.vilkaarsvurdering.klienter.GrunnlagKlientImpl

class ApplicationContext {
    val config: Config = ConfigFactory.load()
    val properties: ApplicationProperties = ApplicationProperties.fromEnv(Miljoevariabler.systemEnv())
    val dataSource = DataSourceBuilder.createDataSource(properties)
    val behandlingKlient = BehandlingKlientImpl(config, httpClient())
    private val delvilkaarRepository = DelvilkaarRepository()
    private val vilkaarsvurderingRepository = VilkaarsvurderingRepository(dataSource, delvilkaarRepository)
    val vilkaarsvurderingService =
        VilkaarsvurderingService(
            vilkaarsvurderingRepository = vilkaarsvurderingRepository,
            behandlingKlient = behandlingKlient,
            grunnlagKlient = GrunnlagKlientImpl(config, httpClient()),
        )
    val aldersovergangService = AldersovergangService(vilkaarsvurderingService)
    private val env: Miljoevariabler = Miljoevariabler.systemEnv()
    private val leaderElectionKlient = LeaderElection(env[ELECTOR_PATH], httpClient())
    private val migrertYrkesskadeOppdaterer = MigrertYrkesskadeOppdaterer(behandlingKlient, vilkaarsvurderingRepository)
    val migrertYrkesskadeJob = MigrertYrkesskadeJob(migrertYrkesskadeOppdaterer) { leaderElectionKlient.isLeader() }
}
