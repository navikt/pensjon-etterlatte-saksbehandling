import com.typesafe.config.ConfigFactory
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.routing
import nav.no.etterlatte.beregning
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.model.BeregningService
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.security.token.support.v2.tokenValidationSupport
import org.slf4j.event.Level
import java.util.*

class ApplicationBuilder {
    private val env = System.getenv()
    private val properties: ApplicationProperties = ApplicationProperties.fromEnv(env)
    private val dataSourceBuilder = DataSourceBuilder(
        jdbcUrl = properties.jdbcUrl,
        username = properties.dbUsername,
        password = properties.dbPassword
    ).apply { migrate() }

    private val dataSource = dataSourceBuilder.dataSource()
    private val beregningRepository = BeregningRepositoryImpl(dataSource)
    private val beregningService = BeregningService(beregningRepository)
    private val rapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env.withConsumerGroupId()))
            .withKtorModule {
                restModule(
                    beregningService = beregningService
                )
            }
            .build()
            .also { LesBeregningsmelding(it, beregningService) }

    fun start() = rapidsConnection.start()

    private fun publiser(melding: String, key: UUID) {
        rapidsConnection.publish(message = melding, key = key.toString())
    }
}

fun Application.restModule(
    beregningService: BeregningService
) {
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }

    install(IgnoreTrailingSlash)

    install(CallLogging) {
        level = Level.INFO
        filter { call -> !call.request.path().matches(Regex(".*/isready|.*/isalive|.*/metrics")) }
        format { call -> "<- ${call.response.status()?.value} ${call.request.httpMethod.value} ${call.request.path()}" }
        mdc(CORRELATION_ID) { call -> call.request.header(X_CORRELATION_ID) ?: UUID.randomUUID().toString() }
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("En feil oppstod: ${cause.message}", cause)
            call.respond(HttpStatusCode.InternalServerError, "En intern feil har oppstått")
        }
    }

    install(Authentication) {
        tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load()))
    }

    routing {
        authenticate {
            beregning(beregningService)
        }
    }
}

fun Map<String, String>.withConsumerGroupId() =
    this.toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }