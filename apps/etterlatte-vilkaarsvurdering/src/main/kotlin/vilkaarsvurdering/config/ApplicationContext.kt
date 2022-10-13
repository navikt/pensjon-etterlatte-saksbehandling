package no.nav.etterlatte.vilkaarsvurdering.config

import no.nav.etterlatte.vilkaarsvurdering.GrunnlagEndretRiver
import no.nav.etterlatte.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepositoryInMemory
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.etterlatte.vilkaarsvurdering.barnepensjon.barnepensjonVilkaar
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

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

    var vilkaarsvurderingRepository = VilkaarsvurderingRepositoryInMemory().apply {
        // legger inn mockdata
        lagre(
            Vilkaarsvurdering(
                behandlingId = UUID.fromString("9a95a795-7f16-4380-b73a-32d4b41445ef"),
                payload = "json",
                vilkaar = barnepensjonVilkaar()
            )
        )
    }

    var vilkaarsvurderingService = VilkaarsvurderingService(vilkaarsvurderingRepository)

    fun grunnlagEndretRiver(rapidsConnection: RapidsConnection): GrunnlagEndretRiver =
        GrunnlagEndretRiver(rapidsConnection, vilkaarsvurderingService)
}

private fun jdbcUrl(host: String, port: Int, databaseName: String) =
    "jdbc:postgresql://$host:$port/$databaseName"