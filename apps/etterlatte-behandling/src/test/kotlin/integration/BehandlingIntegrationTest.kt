package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.fullPath
import io.mockk.spyk
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.kafka.TestProdusent
import no.nav.etterlatte.ktor.issueSaksbehandlerToken
import no.nav.etterlatte.ktor.issueSystembrukerToken
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.extension.RegisterExtension

abstract class BehandlingIntegrationTest {
    companion object {
        @RegisterExtension
        private val dbExtension = DatabaseExtension()
    }

    protected val server: MockOAuth2Server = MockOAuth2Server()
    internal lateinit var applicationContext: ApplicationContext

    protected fun startServer(
        norg2Klient: Norg2Klient? = null,
        featureToggleService: FeatureToggleService = DummyFeatureToggleService(),
        brevApiKlient: BrevApiKlient? = null,
        pdlTjenesterKlient: PdlTjenesterKlient? = null,
    ) {
        server.start()
        val props = dbExtension.properties()

        applicationContext =
            ApplicationContext(
                env =
                    System.getenv().toMutableMap().apply {
                        put("KAFKA_RAPID_TOPIC", "test")
                        put("DB_HOST", props.host)
                        put("DB_USERNAME", props.username)
                        put("DB_PASSWORD", props.password)
                        put("DB_PORT", props.firstMappedPort.toString())
                        put("DB_DATABASE", props.databaseName)
                        put("AZUREAD_ATTESTANT_GROUPID", azureAdAttestantClaim)
                        put("AZUREAD_ATTESTANT_GJENNY_GROUPID", azureAdAttestantGjennyClaim)
                        put("AZUREAD_SAKSBEHANDLER_GROUPID", azureAdSaksbehandlerClaim)
                        put("AZUREAD_STRENGT_FORTROLIG_GROUPID", azureAdStrengtFortroligClaim)
                        put("AZUREAD_EGEN_ANSATT_GROUPID", azureAdEgenAnsattClaim)
                        put("AZUREAD_FORTROLIG_GROUPID", azureAdFortroligClaim)
                        put("AZUREAD_NASJONAL_TILGANG_UTEN_LOGG_GROUPID", azureAdNasjonUtenLoggClaim)
                        put("AZUREAD_NASJONAL_TILGANG_MED_LOGG_GROUPID", azureAdNasjonMedLoggClaim)
                        put("NORG2_URL", "http://localhost")
                        put("NAVANSATT_URL", "http://localhost")
                        put("SKJERMING_URL", "http://localhost")
                        put("OPPGAVE_URL", "http://localhost")
                        put("PEN_URL", "http://localhost")
                        put("PEN_CLIENT_ID", "ddd52335-cfe8-4ee9-9e68-416a5ab26efa")
                        put("ETTERLATTE_KLAGE_API_URL", "http://localhost")
                        put("ETTERLATTE_TILBAKEKREVING_URL", "http://localhost")
                        put("ETTERLATTE_MIGRERING_URL", "http://localhost")
                        put("OPPGAVE_SCOPE", "scope")
                    }.let { Miljoevariabler(it) },
                config =
                    ConfigFactory.parseMap(
                        mapOf(
                            "pdltjenester.url" to "http://localhost",
                            "grunnlag.resource.url" to "http://localhost",
                            "vedtak.resource.url" to "http://localhost",
                            "krr.url" to "http://localhost",
                        ),
                    ),
                rapid = TestProdusent(),
                featureToggleService = featureToggleService,
                skjermingHttpKlient = skjermingHttpClient(),
                grunnlagHttpClient = grunnlagHttpClient(),
                leaderElectionHttpClient = leaderElection(),
                navAnsattKlient = NavAnsattKlientTest(),
                norg2Klient = norg2Klient ?: Norg2KlientTest(),
                grunnlagKlientObo = GrunnlagKlientTest(),
                vedtakKlient = spyk(VedtakKlientTest()),
                beregningsKlient = BeregningKlientTest(),
                gosysOppgaveKlient = GosysOppgaveKlientTest(),
                brevApiKlient = brevApiKlient ?: BrevApiKlientTest(),
                klageHttpClient = klageHttpClientTest(),
                tilbakekrevingHttpClient = tilbakekrevingHttpClientTest(),
                migreringHttpClient = migreringHttpClientTest(),
                pesysKlient = PesysKlientTest(),
                krrKlient = KrrklientTest(),
                axsysKlient = AxsysKlientTest(),
                pdlTjenesterKlient = pdlTjenesterKlient ?: PdltjenesterKlientTest(),
            )
    }

    fun leaderElection() =
        HttpClient(MockEngine) {
            engine {
                addHandler { req ->
                    if (req.url.fullPath == "electorPath") {
                        respond("me")
                    } else {
                        error(req.url.fullPath)
                    }
                }
            }
        }

    fun resetDatabase() {
        dbExtension.resetDb()
    }

    protected fun afterAll() {
        applicationContext.close()
        server.shutdown()
    }

    protected fun HttpRequestBuilder.addAuthToken(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    protected val tokenSaksbehandler: String by lazy {
        server.issueSaksbehandlerToken(navn = "John Doe", navIdent = "Saksbehandler01", groups = listOf(azureAdAttestantClaim))
    }

    protected val tokenSaksbehandler2: String by lazy {
        server.issueSaksbehandlerToken(navn = "Jane Doe", navIdent = "Saksbehandler02", groups = listOf(azureAdAttestantClaim))
    }

    protected val fagsystemTokenEY: String by lazy { server.issueSystembrukerToken() }

    protected val tokenAttestant: String by lazy {
        server.issueSaksbehandlerToken(
            navn = "John Doe",
            navIdent = "Saksbehandler02",
            groups = listOf(azureAdSaksbehandlerClaim, azureAdAttestantClaim),
        )
    }

    protected val tokenSaksbehandlerMedStrengtFortrolig: String by lazy {
        server.issueSaksbehandlerToken(
            navn = "John Doe",
            navIdent = "saksebehandlerstrengtfortrolig",
            groups =
                listOf(
                    azureAdSaksbehandlerClaim,
                    azureAdAttestantClaim,
                    azureAdStrengtFortroligClaim,
                ),
        )
    }

    protected val tokenSaksbehandlerMedEgenAnsattTilgang: String by lazy {
        server.issueSaksbehandlerToken(
            navn = "John Doe",
            navIdent = "saksbehandlerskjermet",
            groups =
                listOf(
                    azureAdAttestantClaim,
                    azureAdEgenAnsattClaim,
                    azureAdSaksbehandlerClaim,
                ),
        )
    }

    protected val systemBruker: String by lazy { server.issueSystembrukerToken() }
}
