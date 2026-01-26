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
import no.nav.etterlatte.behandling.klienter.TilbakekrevingKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.brev.BrevKlient
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.SkjermingKlient
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.kafka.KafkaKey
import no.nav.etterlatte.kafka.TestProdusent
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.ktor.token.issueSystembrukerToken
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.database.DatabaseConfig
import no.nav.etterlatte.tilgangsstyring.AzureKey
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.extension.RegisterExtension

abstract class BehandlingIntegrationTest {
    companion object {
        @RegisterExtension
        private val dbExtension = DatabaseExtension()
    }

    protected val mockOAuth2Server: MockOAuth2Server = MockOAuth2Server()
    internal lateinit var applicationContext: ApplicationContext

    protected val saksbehandlerIdent = "Saksbehandler01"
    protected val attestantIdent = "Saksbehandler02"
    protected val saksbehandlerStrengtFortroligIdent = "saksebehandlerstrengtfortrolig"
    protected val saksbehandlerSkjermetIdent = "saksbehandlerskjermet"

    protected fun startServer(
        norg2Klient: Norg2Klient? = null,
        featureToggleService: FeatureToggleService = DummyFeatureToggleService(),
        brevApiKlient: BrevApiKlient? = null,
        brevKlient: BrevKlient? = null,
        pdlTjenesterKlient: PdlTjenesterKlient? = null,
        tilbakekrevingKlient: TilbakekrevingKlient? = null,
        testProdusent: TestProdusent<String, String>? = null,
        skjermingKlient: SkjermingKlient? = null,
        vedtakKlient: VedtakKlient? = null,
        grunnlagService: GrunnlagService? = null,
    ) {
        mockOAuth2Server.start()
        val props = dbExtension.properties()

        var systemEnv = Miljoevariabler.systemEnv()
        mapOf(
            KafkaKey.KAFKA_RAPID_TOPIC to "test",
            DatabaseConfig.DB_HOST to props.host,
            DatabaseConfig.DB_USERNAME to props.username,
            DatabaseConfig.DB_PASSWORD to props.password,
            DatabaseConfig.DB_PORT to props.firstMappedPort.toString(),
            DatabaseConfig.DB_DATABASE to props.databaseName,
            AzureKey.AZUREAD_ATTESTANT_GROUPID to azureAdAttestantClaim,
            AzureKey.AZUREAD_ATTESTANT_GJENNY_GROUPID to azureAdAttestantGjennyClaim,
            AzureKey.AZUREAD_SAKSBEHANDLER_GROUPID to azureAdSaksbehandlerClaim,
            AzureKey.AZUREAD_STRENGT_FORTROLIG_GROUPID to azureAdStrengtFortroligClaim,
            AzureKey.AZUREAD_EGEN_ANSATT_GROUPID to azureAdEgenAnsattClaim,
            AzureKey.AZUREAD_FORTROLIG_GROUPID to azureAdFortroligClaim,
            AzureKey.AZUREAD_NASJONAL_TILGANG_UTEN_LOGG_GROUPID to azureAdNasjonUtenLoggClaim,
            AzureKey.AZUREAD_NASJONAL_TILGANG_MED_LOGG_GROUPID to azureAdNasjonMedLoggClaim,
            EnvKey.NORG2_URL to "http://localhost",
            EnvKey.NAVANSATT_URL to "http://localhost",
            EnvKey.SKJERMING_URL to "http://localhost",
            TestEnvKey.OPPGAVE_URL to "http://localhost",
            TestEnvKey.PEN_URL to "http://localhost",
            TestEnvKey.PEN_CLIENT_ID to "ddd52335-cfe8-4ee9-9e68-416a5ab26efa",
            EnvKey.ETTERLATTE_KLAGE_API_URL to "http://localhost",
            EnvKey.ETTERLATTE_TILBAKEKREVING_URL to "http://localhost",
            EnvKey.ETTERLATTE_MIGRERING_URL to "http://localhost",
            TestEnvKey.OPPGAVE_SCOPE to "scope",
        ).forEach { i -> systemEnv = systemEnv.append(i.key, { i.value }) }
        applicationContext =
            ApplicationContext(
                env = systemEnv,
                config =
                    ConfigFactory.parseMap(
                        mapOf(
                            "pdltjenester.url" to "http://localhost",
                            "grunnlag.resource.url" to "http://localhost",
                            "vedtak.resource.url" to "http://localhost",
                            "krr.url" to "http://localhost",
                            "arbeidOgInntekt.url" to "http://localhost",
                            "azure.app.well.known.url" to "wellKnownUrl",
                            "brev-api.resource.url" to "http://localhost",
                        ),
                    ),
                rapid = testProdusent ?: TestProdusent(),
                featureToggleService = featureToggleService,
                skjermingKlient = skjermingKlient ?: SkjermingKlientTest(),
                leaderElectionHttpClient = leaderElection(),
                navAnsattKlient = NavAnsattKlientTest(),
                norg2Klient = norg2Klient ?: Norg2KlientTest(),
                vedtakKlient = vedtakKlient ?: spyk(VedtakKlientTest()),
                beregningKlient = BeregningKlientTest(),
                trygdetidKlient = TrygdetidKlientTest(),
                gosysOppgaveKlient = GosysOppgaveKlientTest(),
                brevApiKlient = brevApiKlient ?: BrevApiKlientTest(),
                brevKlient = brevKlient ?: BrevKlientTest(),
                klageHttpClient = klageHttpClientTest(),
                tilbakekrevingKlient = tilbakekrevingKlient ?: TilbakekrevingKlientTest(),
                pesysKlient = PesysKlientTest(),
                krrKlient = KrrklientTest(),
                entraProxyKlient = EntraProxyKlientTest(),
                pdlTjenesterKlient = pdlTjenesterKlient ?: PdltjenesterKlientTest(),
                kodeverkKlient = KodeverkKlientTest(),
                inntektskomponentKlient = InntektskomponentKlientTest(),
                grunnlagServiceOverride = grunnlagService,
                sigrunKlient = SigrunKlienTest(),
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
        mockOAuth2Server.shutdown()
    }

    protected fun HttpRequestBuilder.addAuthToken(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    protected val tokenSaksbehandler: String by lazy {
        mockOAuth2Server.issueSaksbehandlerToken(
            navn = "John Doe",
            navIdent = saksbehandlerIdent,
            groups = listOf(azureAdAttestantClaim),
        )
    }

    protected val tokenAttestant: String by lazy {
        mockOAuth2Server.issueSaksbehandlerToken(
            navn = "John Doe",
            navIdent = attestantIdent,
            groups = listOf(azureAdSaksbehandlerClaim, azureAdAttestantClaim),
        )
    }

    protected val tokenSaksbehandlerMedStrengtFortrolig: String by lazy {
        mockOAuth2Server.issueSaksbehandlerToken(
            navn = "John Doe",
            navIdent = saksbehandlerStrengtFortroligIdent,
            groups =
                listOf(
                    azureAdSaksbehandlerClaim,
                    azureAdAttestantClaim,
                    azureAdStrengtFortroligClaim,
                ),
        )
    }

    protected val tokenSaksbehandlerMedEgenAnsattTilgang: String by lazy {
        mockOAuth2Server.issueSaksbehandlerToken(
            navn = "John Doe",
            navIdent = saksbehandlerSkjermetIdent,
            groups =
                listOf(
                    azureAdAttestantClaim,
                    azureAdEgenAnsattClaim,
                    azureAdSaksbehandlerClaim,
                ),
        )
    }

    protected val systemBruker: String by lazy { mockOAuth2Server.issueSystembrukerToken() }
}

enum class TestEnvKey : EnvEnum {
    PEN_CLIENT_ID,
    PEN_URL,
    OPPGAVE_URL,
    OPPGAVE_SCOPE,
    ;

    override fun key() = name
}
