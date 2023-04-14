package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.etterlatte.behandling.EnhetService
import no.nav.etterlatte.behandling.EnhetServiceImpl
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.config.CommonFactory
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleServiceProperties
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.jobs.LeaderElection
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

fun main() {
    /*
    Krever kjørende docker
    Spinner opp appen uten sikkerhet (inkommende token blir godtatt uten validering)
    Kaller vilkårsvurdering i dev
     */

    val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")
    postgreSQLContainer.start()
    postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
    postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)
    val azureAdAttestantClaim: String by lazy {
        "0af3955f-df85-4eb0-b5b2-45bf2c8aeb9e"
    }

    val azureAdSaksbehandlerClaim: String by lazy {
        "63f46f74-84a8-4d1c-87a8-78532ab3ae60"
    }
    Server(
        LocalAppBeanFactory(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password,
            azureAdAttestantClaim = azureAdAttestantClaim,
            azureAdSaksbehandlerClaim = azureAdSaksbehandlerClaim
        )
    ).run()
    postgreSQLContainer.stop()
}

class LocalAppBeanFactory(
    private val jdbcUrl: String,
    private val username: String,
    private val password: String,
    private val azureAdSaksbehandlerClaim: String,
    private val azureAdAttestantClaim: String
) : CommonFactory() {

    override fun getSaksbehandlerGroupIdsByKey(): Map<String, String> =
        mapOf(
            "AZUREAD_ATTESTANT_GROUPID" to azureAdAttestantClaim,
            "AZUREAD_SAKSBEHANDLER_GROUPID" to azureAdSaksbehandlerClaim,
            "AZUREAD_STRENGT_FORTROLIG_GROUPID" to "5ef775f2-61f8-4283-bf3d-8d03f428aa14",
            "AZUREAD_FORTROLIG_GROUPID" to "ea930b6b-9397-44d9-b9e6-f4cf527a632a"
        )

    override fun dataSource(): DataSource = DataSourceBuilder.createDataSource(jdbcUrl, username, password)
    override fun rapid(): KafkaProdusent<String, String> {
        return object : KafkaProdusent<String, String> {
            override fun publiser(noekkel: String, verdi: String, headers: Map<String, ByteArray>?): Pair<Int, Long> {
                return 0 to 0
            }
        }
    }

    override fun pdlHttpClient(): HttpClient =
        HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    if (request.url.fullPath.startsWith("/")) {
                        val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        val json = javaClass.getResource("")!!.readText() // TODO: endre name
                        respond(json, headers = headers)
                    } else {
                        error(request.url.fullPath)
                    }
                }
            }
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
        }

    override fun grunnlagHttpClient(): HttpClient =
        HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    if (request.url.fullPath.startsWith("/")) {
                        val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        respond(Grunnlag.empty().toJson(), headers = headers)
                    } else {
                        error(request.url.fullPath)
                    }
                }
            }
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
        }

    override fun vedtakKlient(): VedtakKlient {
        return VedtakKlientTest()
    }

    override fun vilkaarsvurderingKlient(): VilkaarsvurderingKlient {
        TODO("Not yet implemented")
    }

    override fun grunnlagKlient(): GrunnlagKlient {
        return GrunnlagKlientTest()
    }

    override fun leaderElection() = LeaderElection(
        electorPath = "electorPath",
        httpClient = HttpClient(MockEngine) {
            engine {
                addHandler { req ->
                    if (req.url.fullPath == "electorPath") {
                        respond("me")
                    } else {
                        error(req.url.fullPath)
                    }
                }
            }
        },
        me = "me"
    )

    override fun featureToggleService(): FeatureToggleService {
        return FeatureToggleService.initialiser(
            mapOf(
                FeatureToggleServiceProperties.ENABLED.navn to "false"
            )
        )
    }

    override fun norg2HttpClient(): Norg2Klient {
        return Norg2KlientTest()
    }

    override fun navAnsattKlient(): NavAnsattKlient {
        return NavAnsattKlientTest()
    }

    override fun enhetService(): EnhetService {
        return EnhetServiceImpl(navAnsattKlient())
    }
}