package no.nav.etterlatte

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.HoconApplicationConfig
import no.nav.etterlatte.grunnlag.GrunnlagHendelser
import no.nav.etterlatte.grunnlag.MigreringHendelser
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.grunnlag.RealGrunnlagService
import no.nav.etterlatte.grunnlag.grunnlagRoute
import no.nav.etterlatte.klienter.BehandlingKlientImpl
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.libs.sporingslogg.Sporingslogg
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val sikkerLogg: Logger = LoggerFactory.getLogger("sikkerLogg")

fun main() {
    val application = ApplicationBuilder()
    application.start()
}

class ApplicationBuilder {
    init {
        sikkerLogg.info("SikkerLogg: etterlatte-grunnlag oppstart")
    }

    private val env = System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", requireNotNull(get("NAIS_APP_NAME")).replace("-", ""))
    }
    private val ds = DataSourceBuilder.createDataSource(env).also { it.migrate() }

    private val config: Config = ConfigFactory.load()
    private val opplysningDao = OpplysningDao(ds)
    private val behandlingKlient = BehandlingKlientImpl(config, httpClient())
    private val grunnlagService = RealGrunnlagService(opplysningDao, Sporingslogg())

    private val rapidsConnection = RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env))
        .withKtorModule {
            restModule(sikkerLogg, routePrefix = "api", config = HoconApplicationConfig(config)) {
                grunnlagRoute(grunnlagService, behandlingKlient)
            }
        }
        .build().apply {
            GrunnlagHendelser(this, grunnlagService)
            MigreringHendelser(this, grunnlagService)
        }

    fun start() = setReady().also { rapidsConnection.start() }
}