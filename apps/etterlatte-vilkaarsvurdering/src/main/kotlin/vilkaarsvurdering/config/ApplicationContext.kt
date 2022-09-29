package no.nav.etterlatte.vilkaarsvurdering.config

import com.typesafe.config.ConfigFactory
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.config.HoconApplicationConfig
import no.nav.etterlatte.vilkaarsvurdering.GrunnlagEndretRiver
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepositoryInMemory
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.security.token.support.v2.tokenValidationSupport

class ApplicationContext(
    val properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv())
) {
    /*var dataSourceBuilder = DataSourceBuilder(
        jdbcUrl = jdbcUrl(
            host = properties.dbHost,
            port = properties.dbPort,
            databaseName = properties.dbName
        ),
        username = properties.dbUsername,
        password = properties.dbPassword
    )

    var dataSource = dataSourceBuilder.dataSource()
    */
    var vilkaarsvurderingRepository = VilkaarsvurderingRepositoryInMemory()

    var vilkaarsvurderingService = VilkaarsvurderingService(vilkaarsvurderingRepository)

    var tokenValidering: AuthenticationConfig.() -> Unit =
        { tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load())) }

    fun grunnlagEndretRiver(rapidsConnection: RapidsConnection): GrunnlagEndretRiver =
        GrunnlagEndretRiver(rapidsConnection, vilkaarsvurderingService)
}

private fun jdbcUrl(host: String, port: Int, databaseName: String) =
    "jdbc:postgresql://$host:$port/$databaseName"