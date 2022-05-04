package no.nav.etterlatte

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.*
import io.ktor.request.header
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.*
import no.nav.etterlatte.database.DatabaseContext
import no.nav.etterlatte.grunnlag.GrunnlagFactory
import no.nav.etterlatte.grunnlag.GrunnlagHendelser
import no.nav.etterlatte.grunnlag.grunnlagRoutes
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.event.Level
import java.util.*
import javax.sql.DataSource

fun main() {
    ventPaaNettverk()
    val env = System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", requireNotNull(get("NAIS_APP_NAME")).replace("-", ""))
    }
    val beanFactory = EnvBasedBeanFactory(env)

    beanFactory.datasourceBuilder().migrate()

    RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env)).withKtorModule{
        module(beanFactory)
    }.build().apply {
        GrunnlagHendelser(this, GrunnlagFactory(beanFactory.grunnlagDao(), beanFactory.opplysningDao()), beanFactory.datasourceBuilder().dataSource)
    }.start()
}

fun Application.module(beanFactory: BeanFactory){

    install(ContentNegotiation) {
        jackson{
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
    install(Authentication) {
        beanFactory.tokenValidering()()
    }
    install(CallLogging) {
        level = Level.INFO
        filter { call -> !call.request.path().startsWith("/internal") }
        format { call -> "<- ${call.response.status()?.value} ${call.request.httpMethod.value} ${call.request.path()}" }
        mdc(CORRELATION_ID) { call -> call.request.header(X_CORRELATION_ID) ?: UUID.randomUUID().toString() }
    }
    install(StatusPages) {
        exception<Throwable> { cause ->
            log.error("En feil oppstod: ${cause.message}", cause)
            call.respond(HttpStatusCode.InternalServerError, "En feil oppstod: ${cause.message}")
        }
    }

    routing {
        authenticate {
            attachContekst(beanFactory.datasourceBuilder().dataSource)
            grunnlagRoutes(beanFactory.grunnlagsService())
        }

    }
}

private fun Route.attachContekst(ds: DataSource){
    intercept(ApplicationCallPipeline.Call) {
        val requestContekst = Context(decideUser(call.principal()!!), DatabaseContext(ds))
        withContext(
            Dispatchers.Default + Kontekst.asContextElement(
                value = requestContekst
            )
        ) {
            proceed()
        }
        Kontekst.remove()
    }
}

private fun ventPaaNettverk() {
    runBlocking { delay(5000) }
}

