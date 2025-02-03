package no.nav.etterlatte

import com.fasterxml.jackson.databind.SerializationFeature
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.routing.route
import no.nav.etterlatte.EnvKey.PDLTJENESTER_URL
import no.nav.etterlatte.grunnlag.GrunnlagHenter
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.grunnlag.RealGrunnlagService
import no.nav.etterlatte.grunnlag.aldersovergang.AldersovergangDao
import no.nav.etterlatte.grunnlag.aldersovergang.AldersovergangService
import no.nav.etterlatte.grunnlag.aldersovergang.aldersovergangRoutes
import no.nav.etterlatte.grunnlag.behandlingGrunnlagRoute
import no.nav.etterlatte.grunnlag.klienter.BehandlingKlientImpl
import no.nav.etterlatte.grunnlag.klienter.PdlTjenesterKlientImpl
import no.nav.etterlatte.grunnlag.personRoute
import no.nav.etterlatte.grunnlag.rivers.GrunnlagHendelserRiver
import no.nav.etterlatte.grunnlag.rivers.GrunnlagsversjoneringRiver
import no.nav.etterlatte.grunnlag.sakGrunnlagRoute
import no.nav.etterlatte.grunnlag.tmpjobb.GrunnlagJobbDao
import no.nav.etterlatte.grunnlag.tmpjobb.GrunnlagPersongalleriJobb
import no.nav.etterlatte.grunnlag.tmpjobb.GrunnlagPersongalleriService
import no.nav.etterlatte.klienter.GrunnlagBackupKlient
import no.nav.etterlatte.libs.common.isProd
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.libs.ktor.AppConfig.ELECTOR_PATH
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.rapidsandrivers.configFromEnvironment
import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import rapidsandrivers.initRogR
import java.time.Duration
import java.time.temporal.ChronoUnit

val sikkerLogg: Logger = sikkerlogger()

fun main() {
    ApplicationBuilder().init()
}

class ApplicationBuilder {
    private val env = getRapidEnv()
    private val ds = DataSourceBuilder.createDataSource(env).also { it.migrate() }
    private val config: Config = ConfigFactory.load()
    private val pdlTjenester: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = config.getString("azure.app.client.id"),
            azureAppJwk = config.getString("azure.app.jwk"),
            azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
            azureAppScope = config.getString("pdltjenester.azure.scope"),
            ekstraJacksoninnstillinger = { it.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) },
        )
    }
    val leaderElectionHttpClient: HttpClient = httpClient()
    val leaderElectionKlient = LeaderElection(env[ELECTOR_PATH], leaderElectionHttpClient)

    private val pdltjenesterKlient = PdlTjenesterKlientImpl(pdlTjenester, env[PDLTJENESTER_URL]!!)
    private val opplysningDao = OpplysningDao(ds)
    private val grunnlagJobbDao = GrunnlagJobbDao(ds)
    private val behandlingKlient = BehandlingKlientImpl(config, httpClient())
    private val grunnlagHenter = GrunnlagHenter(pdltjenesterKlient)
    private val grunnlagService =
        RealGrunnlagService(pdltjenesterKlient, opplysningDao, grunnlagHenter)

    private val aldersovergangDao = AldersovergangDao(ds)
    private val aldersovergangService = AldersovergangService(aldersovergangDao)

    private val grunnlagBackupClientCredentials: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = config.getString("azure.app.client.id"),
            azureAppJwk = config.getString("azure.app.jwk"),
            azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
            azureAppScope = config.getString("grunnlagbackup.azure.scope"),
        )
    }
    private val grunnagBackupKlient = GrunnlagBackupKlient(grunnlagBackupClientCredentials)
    private val grunnlagPersongalleriService = GrunnlagPersongalleriService(grunnlagJobbDao, grunnagBackupKlient)

    val grunnlagPersongalleriJobb: GrunnlagPersongalleriJobb by lazy {
        GrunnlagPersongalleriJobb(
            grunnlagPersongalleriService,
            { leaderElectionKlient.isLeader() },
            Duration.of(10, ChronoUnit.SECONDS).toMillis(),
            interval = if (isProd()) Duration.of(1, ChronoUnit.DAYS) else Duration.of(1, ChronoUnit.DAYS),
        )
    }

    fun init() =
        initRogR(
            applikasjonsnavn = "grunnlag",
            restModule = {
                restModule(sikkerLogg, routePrefix = "api", config = HoconApplicationConfig(config)) {
                    route("grunnlag") {
                        sakGrunnlagRoute(grunnlagService, behandlingKlient)
                        behandlingGrunnlagRoute(grunnlagService, behandlingKlient)
                        personRoute(grunnlagService, behandlingKlient)
                        aldersovergangRoutes(behandlingKlient, aldersovergangService)
                    }
                }
            },
            configFromEnvironment = { configFromEnvironment(it) },
        ) { rapidsConnection, _ ->
            GrunnlagsversjoneringRiver(rapidsConnection, grunnlagService)
            GrunnlagHendelserRiver(rapidsConnection, grunnlagService)
            rapidsConnection.register(
                object : RapidsConnection.StatusListener {
                    override fun onStartup(rapidsConnection: RapidsConnection) {
                        val timer = grunnlagPersongalleriJobb.schedule()
                        addShutdownHook(timer)
                    }
                },
            )
        }
}
