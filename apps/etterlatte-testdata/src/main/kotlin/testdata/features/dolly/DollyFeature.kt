package testdata.features.dolly


import com.typesafe.config.Config
import dolly.DollyClient
import dolly.DollyClientImpl
import dolly.DollyService
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.mustache.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.etterlatte.TestDataFeature
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.logger
import java.util.*

class DollyFeature(val config: Config) : TestDataFeature {
    override val beskrivelse: String
        get() = "Opprett søknad"
    override val path: String
        get() = "dolly"
    override val routes: Route.() -> Unit
        get() = {
            get {

                val dollyService = DollyService(
                    dollyClient = DollyClientImpl(config, httpClient())
                )

                // Sjekk om gruppe finnes
                val httpClient = httpClient()
                val azureAdClient = AzureAdClient(config, httpClient)
                val token = azureAdClient.getAccessTokenForResource(listOf("api://${config.getString("dolly.client.id")}/.default"))

                val gruppeId = dollyService.hentTestGruppe("Emil.Schroder@nav.no", token.accessToken)

                logger.info(gruppeId.toString())

                call.respond(
                    MustacheContent(
                        "soeknad/dolly.hbs", mapOf(
                            "beskrivelse" to beskrivelse,
                            "path" to path,
                            "gruppeId" to gruppeId
                        )
                    )
                )
            }

            post {
                try {
                    call.respondRedirect("/$path/sendt")
                } catch (e: Exception) {
                    logger.error("En feil har oppstått! ", e)

                    call.respond(
                        MustacheContent(
                            "error.hbs",
                            mapOf("errorMessage" to e.message, "stacktrace" to e.stackTraceToString())
                        )
                    )
                }
            }

            get("sendt") {
                val partisjon = call.request.queryParameters["partisjon"]!!
                val offset = call.request.queryParameters["offset"]!!

                call.respond(
                    MustacheContent(
                        "soeknad/soeknad-sendt.hbs", mapOf(
                            "path" to path,
                            "beskrivelse" to beskrivelse,
                            "partisjon" to partisjon,
                            "offset" to offset
                        )
                    )
                )
            }

            get("hent-familie") {

                val res: String = try {
                    // todo: tester ut integrasjon mot Dolly.
                    val httpClient = httpClient()
                    val azureAdClient = AzureAdClient(config, httpClient)
                    val token = azureAdClient.getAccessTokenForResource(listOf("api://${config.getString("dolly.client.id")}/.default"))

                    logger.info("client access token: " + token.accessToken)

                    val res = httpClient.get("https://dolly-backend.dev-fss-pub.nais.io/api/v1/bruker") {
                        header("Authorization", "Bearer ${token.accessToken}")
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header("Nav-Consumer-Id", "etterlatte-testdata")
                        header("Nav-Call-Id", UUID.randomUUID().toString())
                    }

                    logger.info(res.bodyAsText())

                    res.bodyAsText()
                } catch (ex: Exception) {
                    logger.error("Klarte ikke hente mal", ex)
                    ex.toString() + ex.stackTraceToString()
                }

                call.respond(res.toJson())
            }
        }
}

private fun httpClient() = HttpClient(OkHttp) {
    expectSuccess = true
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(no.nav.etterlatte.libs.common.objectMapper))
    }
    defaultRequest {
        header(X_CORRELATION_ID, getCorrelationId())
    }
}.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }

data class Familie (
    val antall: Number,
    val navn: String
)