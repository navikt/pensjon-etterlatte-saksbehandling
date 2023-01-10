package no.nav.etterlatte

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.grunnlag.BehandlingEndretHendlese
import no.nav.etterlatte.grunnlag.BehandlingHendelser
import no.nav.etterlatte.grunnlag.GrunnlagHendelser
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.grunnlag.RealGrunnlagService
import no.nav.etterlatte.grunnlag.grunnlagRoute
import no.nav.etterlatte.klienter.BehandlingKlientImpl
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.helse.rapids_rivers.RapidApplication
import java.util.*

fun main() {
    val application = ApplicationBuilder()
    application.start()
}

class ApplicationBuilder {
    private val env = System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", requireNotNull(get("NAIS_APP_NAME")).replace("-", ""))
    }
    private val ds = DataSourceBuilder(env).also { it.migrate() }

    private val config: Config = ConfigFactory.load()
    private val opplysningDao = OpplysningDao(ds.dataSource)
    private val behandlingKlient = BehandlingKlientImpl(config, httpClient())
    private val grunnlagService = RealGrunnlagService(opplysningDao, ::publiser, behandlingKlient)

    private val rapidsConnection = RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env))
        .withKtorModule { apiModule { grunnlagRoute(grunnlagService) } }
        .build().apply {
            GrunnlagHendelser(this, grunnlagService)
            BehandlingHendelser(this)
            BehandlingEndretHendlese(this, grunnlagService)
        }

    private fun publiser(melding: String, key: UUID) {
        rapidsConnection.publish(message = melding, key = key.toString())
    }
    fun start() = rapidsConnection.start()
}