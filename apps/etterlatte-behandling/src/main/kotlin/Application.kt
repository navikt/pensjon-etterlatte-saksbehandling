package no.nav.etterlatte

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.config.*
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
import no.nav.etterlatte.sak.RealSakService
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.sakRoutes
import no.nav.etterlatte.sikkerhet.tokenTestSupportAcceptsAllTokens
import no.nav.security.token.support.ktor.tokenValidationSupport
import javax.sql.DataSource

fun main() {
    ventPaaNettverk()
    appFromEnv(System.getenv()).start(true)
}

fun appFromEnv(env: Map<String, String>): ApplicationEngine {
    return embeddedServer(CIO, applicationEngineEnvironment {
        modules.add{ module(env) }
        connector { port = 8080 }
    })
}

fun Application.module(env: Map<String, String>){

    val ds = DataSourceBuilder(env)
    ds.migrate()

    val sakService = RealSakService(SakDao{ databaseContext().activeTx()})
    val behandlingService = RealBehandlingService(BehandlingDao { databaseContext().activeTx()}, OpplysningDao { databaseContext().activeTx() })


    install(ContentNegotiation) {
        jackson{
            registerModule(JavaTimeModule())
        }
    }
    install(Authentication) {
        if(env["profil"] == "test") tokenTestSupportAcceptsAllTokens()
        else tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load()))
    }
    routing {
        beregningRoutes()
        naisprobes()
        authenticate {
            attachContekst(ds.dataSource)
            sakRoutes(sakService)
            behandlingRoutes(behandlingService)
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
