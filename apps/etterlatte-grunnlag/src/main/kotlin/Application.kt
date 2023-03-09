package no.nav.etterlatte

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import no.nav.etterlatte.grunnlag.BehandlingHendelser
import no.nav.etterlatte.grunnlag.FoedselsnummerDTO
import no.nav.etterlatte.grunnlag.GrunnlagHendelser
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.grunnlag.RealGrunnlagService
import no.nav.etterlatte.grunnlag.grunnlagRoute
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.klienter.BehandlingKlientImpl
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.helsesjekk.setReady
import no.nav.etterlatte.libs.ktor.bruker
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.restModule
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
    private val grunnlagService = RealGrunnlagService(opplysningDao, behandlingKlient, Sporingslogg())

    private val rapidsConnection = RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env))
        .withKtorModule {
            restModule(sikkerLogg, routePrefix = "api", config = HoconApplicationConfig(config)) {
                adressebeskyttelsesInterceptor(behandlingKlient = behandlingKlient)
                grunnlagRoute(grunnlagService).also { install(DoubleReceive) }
            }
        }
        .build().apply {
            GrunnlagHendelser(this, grunnlagService)
            BehandlingHendelser(this)
        }

    fun start() = setReady().also { rapidsConnection.start() }
}

private fun Route.adressebeskyttelsesInterceptor(behandlingKlient: BehandlingKlient) {
    intercept(ApplicationCallPipeline.Call) {
        if (bruker is no.nav.etterlatte.token.System) {
            return@intercept
        }
        val fnr = call.receive<FoedselsnummerDTO>()
        val erStrengtFortrolig = behandlingKlient.sjekkErStrengtFortrolig(fnr.foedselsnummer)
        if (erStrengtFortrolig) {
            call.respond(HttpStatusCode.NotFound)
        }
        return@intercept
    }
}