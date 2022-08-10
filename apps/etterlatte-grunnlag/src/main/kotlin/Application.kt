package no.nav.etterlatte

import no.nav.etterlatte.grunnlag.*
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val env = System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", requireNotNull(get("NAIS_APP_NAME")).replace("-", ""))
    }
    val ds = DataSourceBuilder(env)
    ds.migrate()

    val opplysningDao = OpplysningDao(ds.dataSource)
    val grunnlagService = RealGrunnlagService(opplysningDao)

    RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env))
        .withKtorModule { apiModule { grunnlagRoute(grunnlagService) } }
        .build().apply {
            GrunnlagHendelser(this, grunnlagService)
            BehandlingHendelser(this)
            BehandlingEndretHendlese(this, grunnlagService)
        }.also { it.start() }
}
