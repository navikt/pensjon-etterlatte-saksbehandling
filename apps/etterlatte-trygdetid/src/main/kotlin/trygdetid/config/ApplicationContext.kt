package no.nav.etterlatte.trygdetid.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.database.ApplicationProperties
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.trygdetid.TrygdetidBeregningService
import no.nav.etterlatte.trygdetid.TrygdetidRepository
import no.nav.etterlatte.trygdetid.TrygdetidServiceImpl
import no.nav.etterlatte.trygdetid.avtale.AvtaleRepository
import no.nav.etterlatte.trygdetid.avtale.AvtaleService
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import no.nav.etterlatte.trygdetid.klienter.GrunnlagKlient
import no.nav.etterlatte.trygdetid.klienter.PesysKlientImpl

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
    val behandlingKlient = BehandlingKlient(config, httpClient())
    val trygdetidService =
        TrygdetidServiceImpl(
            TrygdetidRepository(dataSource),
            behandlingKlient = behandlingKlient,
            grunnlagKlient = grunnlagKlient,
            beregnTrygdetidService = TrygdetidBeregningService,
            pesysKlient = PesysKlientImpl(config, httpClient()),
        )
    private val avtaleRepository = AvtaleRepository(dataSource)
    val avtaleService = AvtaleService(avtaleRepository)
}
