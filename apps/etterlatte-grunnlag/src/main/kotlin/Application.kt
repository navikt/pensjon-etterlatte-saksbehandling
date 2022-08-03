package no.nav.etterlatte

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import no.nav.etterlatte.database.DatabaseContext
import no.nav.etterlatte.grunnlag.BehandlingEndretHendlese
import no.nav.etterlatte.grunnlag.BehandlingHendelser
import no.nav.etterlatte.grunnlag.GrunnlagHendelser
import no.nav.etterlatte.grunnlag.grunnlagRoute
import no.nav.helse.rapids_rivers.RapidApplication

suspend fun main() {
    val env = System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", requireNotNull(get("NAIS_APP_NAME")).replace("-", ""))
    }
    val beanFactory = EnvBasedBeanFactory(env)
    val grunnlag = beanFactory.grunnlagsService()

    beanFactory.datasourceBuilder().migrate()

    RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env))
        .withKtorModule { apiModule { grunnlagRoute(grunnlag) } }
        .build().apply {
            GrunnlagHendelser(this, grunnlag)
            BehandlingHendelser(this)
            BehandlingEndretHendlese(this, grunnlag)
        }.also {
            withContext(
                Dispatchers.Default + Kontekst.asContextElement(
                    value = Context(
                        Self("etterlatte-grunnlag"),
                        DatabaseContext(beanFactory.datasourceBuilder().dataSource)
                    )
                )
            ) {
                it.start()
            }
        }
}
