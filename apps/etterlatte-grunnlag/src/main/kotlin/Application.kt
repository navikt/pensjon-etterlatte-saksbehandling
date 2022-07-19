package no.nav.etterlatte

import kotlinx.coroutines.*
import no.nav.etterlatte.database.DatabaseContext
import no.nav.etterlatte.grunnlag.*
import no.nav.helse.rapids_rivers.RapidApplication

suspend fun main() {
    ventPaaNettverk()
    val env = System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", requireNotNull(get("NAIS_APP_NAME")).replace("-", ""))
    }
    val beanFactory = EnvBasedBeanFactory(env)

    beanFactory.datasourceBuilder().migrate()

    RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env)).build().apply {
        val grunnlag = beanFactory.grunnlagsService()
        GrunnlagHendelser(this, grunnlag)//, beanFactory.datasourceBuilder().dataSource)
        BehandlingHendelser(this)
        BehandlingEndretHendlese(this, grunnlag)
    }.also {
        withContext(Dispatchers.Default + Kontekst.asContextElement(
            value = Context(Self("etterlatte-grunnlag"), DatabaseContext(beanFactory.datasourceBuilder().dataSource))
        )) {
            it.start()
        }
    }
}

private fun ventPaaNettverk() {
    runBlocking { delay(5000) }
}

