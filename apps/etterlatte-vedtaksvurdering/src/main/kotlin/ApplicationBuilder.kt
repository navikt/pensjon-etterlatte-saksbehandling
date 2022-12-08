package no.nav.etterlatte

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
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.vedtaksvurdering.database.VedtaksvurderingRepository
import no.nav.etterlatte.vedtaksvurdering.rivers.AttesterVedtak
import no.nav.etterlatte.vedtaksvurdering.rivers.FattVedtak
import no.nav.etterlatte.vedtaksvurdering.rivers.LagreBeregningsresultat
import no.nav.etterlatte.vedtaksvurdering.rivers.LagreIverksattVedtak
import no.nav.etterlatte.vedtaksvurdering.rivers.LagreVilkaarsresultat
import no.nav.etterlatte.vedtaksvurdering.rivers.UnderkjennVedtak
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.security.token.support.v2.tokenValidationSupport
import org.slf4j.event.Level
import rapidsandrivers.vedlikehold.registrerVedlikeholdsriver
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
    private val vedtakRepo = VedtaksvurderingRepository.using(dataSource)
    private val vedtaksvurderingService = VedtaksvurderingService(vedtakRepo)

    private val rapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env.withConsumerGroupId()))
            .withKtorModule {
                restModule(vedtaksvurderingService)
            }
            .build()
            .apply {
                LagreVilkaarsresultat(this, vedtaksvurderingService)
                LagreBeregningsresultat(this, vedtaksvurderingService)
                FattVedtak(this, vedtaksvurderingService)
                AttesterVedtak(this, vedtaksvurderingService)
                UnderkjennVedtak(this, vedtaksvurderingService)
                LagreIverksattVedtak(this, vedtaksvurderingService)
                registrerVedlikeholdsriver(vedtaksvurderingService)
            }

    fun start() = rapidsConnection.start()
}

fun Application.restModule(
    vedtaksvurderingService: VedtaksvurderingService
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
            vilkaarsvurderingRoute(vedtaksvurderingService)
        }
    }
}

private fun Map<String, String>.withConsumerGroupId() =
    this.toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }