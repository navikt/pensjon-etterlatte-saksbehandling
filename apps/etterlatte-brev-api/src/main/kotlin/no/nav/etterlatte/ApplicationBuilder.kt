package no.nav.etterlatte

import no.nav.etterlatte.adresse.AdresseKlient
import no.nav.etterlatte.adresse.AdresseServiceMock
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.jackson.jackson
import no.nav.etterlatte.brev.BrevService
import no.nav.etterlatte.brev.MottakerService
import no.nav.etterlatte.brev.brevRoute
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.etterlatte.db.BrevRepository
import no.nav.etterlatte.grunnbeloep.GrunnbeloepKlient
import no.nav.etterlatte.journalpost.JournalpostClient
import no.nav.etterlatte.journalpost.JournalpostServiceMock
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.norg2.Norg2Klient
import no.nav.etterlatte.pdf.PdfGeneratorKlient
import no.nav.etterlatte.vedtak.VedtakServiceMock
import no.nav.etterlatte.security.ktor.clientCredential
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

class ApplicationBuilder {
    private val env = System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }
    val config: Config = ConfigFactory.load()
    private val localDevelopment: Boolean = env["BREV_LOCAL_DEV"].toBoolean()
    private val pdfGenerator = PdfGeneratorKlient(httpClient(), env["ETTERLATTE_PDFGEN_URL"]!!)
    private val vedtakService = VedtakServiceMock()
    private val grunnbeloepKlient = GrunnbeloepKlient(httpClient())
    private val norg2Klient = Norg2Klient(env["NORG2_URL"]!!, httpClient())
    private val datasourceBuilder = DataSourceBuilder(env)
    private val db = BrevRepository.using(datasourceBuilder.dataSource)
    private val mottakerService = MottakerService(httpClient(), env["ETTERLATTE_BRREG_URL"]!!)

    private val logger = LoggerFactory.getLogger(ApplicationBuilder::class.java)


    private val adresseService = if (localDevelopment) {
        AdresseServiceMock()
    } else {
        AdresseKlient(regHttpclient(config.getConfig("no.nav.etterlatte.brev.api.aad")), env["REGOPPSLAG_URL"]!!)
    }

    private val brevService = BrevService(
        db,
        pdfGenerator,
        vedtakService,
        norg2Klient,
        grunnbeloepKlient,
        adresseService,
        ::sendToRapid
    )


    private val journalpostService = if (localDevelopment) {
        JournalpostServiceMock()
    } else {
        JournalpostClient(
            httpClient(),
            env["HENT_DOKUMENT_URL"]!!,
            env["SAF_GRAPHQL_URL"]!!
        )
    }

    private val rapidsConnection: RapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env))
            .withKtorModule {
                apiModule(localDevelopment) {
                    brevRoute(brevService, mottakerService, journalpostService)
                }
            }
            .build()
            .apply {
                register(object : RapidsConnection.StatusListener {
                    override fun onStartup(rapidsConnection: RapidsConnection) {
                        datasourceBuilder.migrate()
                    }
                })
                OppdaterDistribusjonStatus(this, db)
            }

    private fun sendToRapid(message: String) = rapidsConnection.publish(message)

    fun start() = rapidsConnection.start()

    private fun httpClient() = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            jackson { objectMapper }
        }
        defaultRequest {
            header(X_CORRELATION_ID, getCorrelationId())
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }

    private fun regHttpclient(aad: Config) = HttpClient(OkHttp) {

        logger.info(aad.toString())
        expectSuccess = true
        val env = mutableMapOf(
            "AZURE_APP_CLIENT_ID" to aad.getString("client_id"),
            "AZURE_APP_WELL_KNOWN_URL" to aad.getString("well_known_url"),
            "AZURE_APP_OUTBOUND_SCOPE" to aad.getString("outbound"),
            "AZURE_APP_JWK" to aad.getString("client_jwk")
        )
        install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
        install(Auth) {
            clientCredential {
                config = env
            }
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }
}
