package no.nav.etterlatte

import com.fasterxml.jackson.databind.SerializationFeature
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.server.routing.route
import no.nav.etterlatte.EnvKey.HTTP_PORT
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
import no.nav.etterlatte.grunnlag.sakGrunnlagRoute
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServer
import no.nav.etterlatte.libs.ktor.initialisering.run
import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import org.slf4j.Logger

val sikkerLogg: Logger = sikkerlogger()

fun main() {
    ApplicationBuilder().run()
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

    private val pdltjenesterKlient = PdlTjenesterKlientImpl(pdlTjenester, env[PDLTJENESTER_URL]!!)
    private val opplysningDao = OpplysningDao(ds)
    private val behandlingKlient = BehandlingKlientImpl(config, httpClient())
    private val grunnlagHenter = GrunnlagHenter(pdltjenesterKlient)
    private val grunnlagService =
        RealGrunnlagService(pdltjenesterKlient, opplysningDao, grunnlagHenter)

    private val aldersovergangDao = AldersovergangDao(ds)
    private val aldersovergangService = AldersovergangService(aldersovergangDao)

    private val engine =
        initEmbeddedServer(
            routePrefix = "/api",
            httpPort = env.getOrDefault(HTTP_PORT, "8080").toInt(),
            applicationConfig = config,
        ) {
            route("grunnlag") {
                sakGrunnlagRoute(grunnlagService, behandlingKlient)
                behandlingGrunnlagRoute(grunnlagService, behandlingKlient)
                personRoute(grunnlagService, behandlingKlient)
                aldersovergangRoutes(behandlingKlient, aldersovergangService)
            }
        }

    fun run() = engine.run()
}
