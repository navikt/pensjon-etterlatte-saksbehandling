package no.nav.etterlatte

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import kotlinx.coroutines.*
import no.nav.etterlatte.behandling.*
import no.nav.etterlatte.beregning.beregningRoutes
import no.nav.etterlatte.database.DatabaseContext
import no.nav.etterlatte.sak.sakRoutes

import javax.sql.DataSource

fun main() {
    ventPaaNettverk()
    appFromEnv(System.getenv()).start(true)
}



fun appFromEnv(env: Map<String, String>): ApplicationEngine {
    return appFromBeanfactory(EnvBasedBeanFactory(env))
}

fun appFromBeanfactory(env: BeanFactory): ApplicationEngine {
    return embeddedServer(CIO, applicationEngineEnvironment {
        modules.add{ module(env) }
        connector { port = 8080 }
    })
}

fun Application.module(beanFactory: BeanFactory){

    val ds = beanFactory.datasourceBuilder().apply {
        migrate()
    }

    install(ContentNegotiation) {
        jackson{
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
    install(Authentication) {
        beanFactory.tokenValidering()()
    }
    routing {
        beregningRoutes()
        naisprobes()
        authenticate {
            attachContekst(ds.dataSource)
            sakRoutes(beanFactory.sakService())
            behandlingRoutes(beanFactory.behandlingService())
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

fun Route.naisprobes(){
    route("internal"){
        get("isalive"){
            call.respondText { "OK" }
        }
        get("isready"){
            call.respondText { "OK" }
        }
        get("started"){
            call.respondText { "OK" }
        }
    }
}
