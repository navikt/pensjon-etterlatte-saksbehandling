package no.nav.etterlatte

import com.fasterxml.jackson.databind.SerializationFeature
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.routing.route
import no.nav.etterlatte.grunnlag.GrunnlagHenter
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.grunnlag.RealGrunnlagService
import no.nav.etterlatte.grunnlag.aldersovergang.AldersovergangDao
import no.nav.etterlatte.grunnlag.aldersovergang.AldersovergangService
import no.nav.etterlatte.grunnlag.aldersovergang.aldersovergangRoutes
import no.nav.etterlatte.grunnlag.behandlingGrunnlagRoute
import no.nav.etterlatte.grunnlag.klienter.BehandlingKlientImpl
import no.nav.etterlatte.grunnlag.klienter.PdlTjenesterKlientImpl
import no.nav.etterlatte.grunnlag.klienter.PersondataKlient
import no.nav.etterlatte.grunnlag.personRoute
import no.nav.etterlatte.grunnlag.rivers.GrunnlagHendelserRiver
import no.nav.etterlatte.grunnlag.rivers.GrunnlagsversjoneringRiver
import no.nav.etterlatte.grunnlag.sakGrunnlagRoute
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.libs.sporingslogg.Sporingslogg
import no.nav.etterlatte.rapidsandrivers.configFromEnvironment
import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger

val sikkerLogg: Logger = sikkerlogger()

fun main() {
    val application = ApplicationBuilder()
    application.start()
}

class ApplicationBuilder {
    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-grunnlag")
    }

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

    val persondataKlient =
        PersondataKlient(
            httpClient =
                httpClientClientCredentials(
                    azureAppClientId = config.getString("azure.app.client.id"),
                    azureAppJwk = config.getString("azure.app.jwk"),
                    azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
                    azureAppScope = config.getString("persondata.outbound.scope"),
                ),
            apiUrl = config.getString("persondata.resource.url"),
        )

    private val pdltjenesterKlient = PdlTjenesterKlientImpl(pdlTjenester, env["PDLTJENESTER_URL"]!!)
    private val opplysningDao = OpplysningDao(ds)
    private val behandlingKlient = BehandlingKlientImpl(config, httpClient())
    private val grunnlagHenter = GrunnlagHenter(pdltjenesterKlient)
    private val grunnlagService =
        RealGrunnlagService(pdltjenesterKlient, opplysningDao, Sporingslogg(), grunnlagHenter)

    private val aldersovergangDao = AldersovergangDao(ds)
    private val aldersovergangService = AldersovergangService(aldersovergangDao)

    private val rapidsConnection =
        RapidApplication
            .Builder(RapidApplication.RapidApplicationConfig.fromEnv(env, configFromEnvironment(env)))
            .withKtorModule {
                restModule(sikkerLogg, routePrefix = "api", config = HoconApplicationConfig(config)) {
                    route("grunnlag") {
                        sakGrunnlagRoute(grunnlagService, behandlingKlient)
                        behandlingGrunnlagRoute(grunnlagService, behandlingKlient)
                        personRoute(grunnlagService, behandlingKlient)
                        aldersovergangRoutes(aldersovergangService)
                    }
                }
            }.build()
            .apply {
                GrunnlagsversjoneringRiver(this, grunnlagService)
                GrunnlagHendelserRiver(this, grunnlagService)
            }

    fun start() = setReady().also { rapidsConnection.start() }
}
