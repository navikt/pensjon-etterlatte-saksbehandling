package no.nav.etterlatte.vilkaarsvurdering.config

import no.nav.etterlatte.vilkaarsvurdering.GrunnlagEndretRiver
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepositoryImpl
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.helse.rapids_rivers.RapidsConnection

class ApplicationContext(
    properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv())
) {
    val dataSourceBuilder = DataSourceBuilder(
        jdbcUrl = properties.jdbcUrl,
        username = properties.dbUsername,
        password = properties.dbPassword
    ).apply { migrate() }

    val dataSource = dataSourceBuilder.dataSource()

    val vilkaarsvurderingRepository = VilkaarsvurderingRepositoryImpl(dataSource)

    val vilkaarsvurderingService = VilkaarsvurderingService(vilkaarsvurderingRepository)

    fun grunnlagEndretRiver(rapidsConnection: RapidsConnection): GrunnlagEndretRiver =
        GrunnlagEndretRiver(rapidsConnection, vilkaarsvurderingService)
}