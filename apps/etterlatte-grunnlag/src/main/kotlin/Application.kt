package no.nav.etterlatte

import com.fasterxml.jackson.databind.SerializationFeature
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.routing.route
import no.nav.etterlatte.grunnlag.GrunnlagHenter
import no.nav.etterlatte.grunnlag.MigreringGrunnlagHendelserRiver
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.grunnlag.RealGrunnlagService
import no.nav.etterlatte.grunnlag.VergeService
import no.nav.etterlatte.grunnlag.aldersovergang.AldersovergangDao
import no.nav.etterlatte.grunnlag.aldersovergang.AldersovergangService
import no.nav.etterlatte.grunnlag.aldersovergang.aldersovergangRoutes
import no.nav.etterlatte.grunnlag.behandlingGrunnlagRoute
import no.nav.etterlatte.grunnlag.klienter.BehandlingKlientImpl
import no.nav.etterlatte.grunnlag.klienter.PdlTjenesterKlientImpl
import no.nav.etterlatte.grunnlag.klienter.PersondataKlient
import no.nav.etterlatte.grunnlag.migreringRoutes
import no.nav.etterlatte.grunnlag.personRoute
import no.nav.etterlatte.grunnlag.rivers.GrunnlagHendelserRiver
import no.nav.etterlatte.grunnlag.rivers.GrunnlagsversjoneringRiver
import no.nav.etterlatte.grunnlag.rivers.InitBehandlingVersjonRiver
import no.nav.etterlatte.grunnlag.rivers.TattAvVentUnder20River
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

    // TODO: Slette så fort grunnlag er mappet
    private val behandlingSystemClient: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = config.getString("azure.app.client.id"),
            azureAppJwk = config.getString("azure.app.jwk"),
            azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
            azureAppScope = config.getString("behandling.azure.scope"),
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
    private val behandlingKlient = BehandlingKlientImpl(config, httpClient(), behandlingSystemClient)
    private val vergeService = VergeService(persondataKlient)
    private val grunnlagHenter = GrunnlagHenter(pdltjenesterKlient, vergeService)
    private val grunnlagService =
        RealGrunnlagService(pdltjenesterKlient, opplysningDao, Sporingslogg(), grunnlagHenter, vergeService)

    private val aldersovergangDao = AldersovergangDao(ds)
    private val aldersovergangService = AldersovergangService(aldersovergangDao)

    private val rapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env))
            .withKtorModule {
                restModule(sikkerLogg, routePrefix = "api", config = HoconApplicationConfig(config)) {
                    route("grunnlag") {
                        sakGrunnlagRoute(grunnlagService, behandlingKlient)
                        behandlingGrunnlagRoute(grunnlagService, behandlingKlient)
                        personRoute(grunnlagService, behandlingKlient)
                        migreringRoutes(persondataKlient, behandlingKlient)
                        aldersovergangRoutes(aldersovergangService)
                    }
                }
            }
            .build().apply {
                GrunnlagsversjoneringRiver(this, grunnlagService)
                InitBehandlingVersjonRiver(this, behandlingKlient, grunnlagService)
                GrunnlagHendelserRiver(this, grunnlagService)
                MigreringGrunnlagHendelserRiver(this, grunnlagService)
                TattAvVentUnder20River(this, aldersovergangService)
            }

    fun start() = setReady().also { rapidsConnection.start() }
}
