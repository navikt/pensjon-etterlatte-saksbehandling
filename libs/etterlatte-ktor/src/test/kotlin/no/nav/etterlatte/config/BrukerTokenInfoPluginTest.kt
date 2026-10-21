package no.nav.etterlatte.config

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.Hook
import io.ktor.server.application.call
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.intercept
import io.ktor.server.routing.route
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.ktor.token.issueSystembrukerToken
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.libs.ktor.token.VanligSystembruker
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

internal class BrukerTokenInfoPluginTest {
    private val oauth = MockOAuth2Server()

    @BeforeEach
    fun start() {
        oauth.startRandomPort()
    }

    @AfterEach
    fun stopp() {
        oauth.shutdown()
    }

    @Test
    fun `brukerTokenInfo er tilgjengelig etter traadbytte og fjernes etter request`() =
        testApplication {
            runServer(oauth) {
                install(AssertPlugin)

                route("/med-ident") {
                    install(BrukerTokenInfoPlugin)
                    get {
                        val bruker = withContext(Dispatchers.IO) { requestBrukerTokenInfo.get() }
                        bruker.shouldBeInstanceOf<Saksbehandler>().ident() shouldBe "Z123456"
                        call.respondText(bruker.ident())
                    }
                    get("/feil") {
                        requestBrukerTokenInfo.get()?.ident() shouldBe "Z123456"
                        error("Forventet feil")
                    }
                }
                get("/uten-ident") {
                    requestBrukerTokenInfo.get() shouldBe null
                    call.respondText("OK")
                }
            }

            val token = oauth.issueSaksbehandlerToken(navIdent = "Z123456")
            client.get("/med-ident") { bearerAuth(token) }.bodyAsText() shouldBe "Z123456"
            client.get("/med-ident/feil") { bearerAuth(token) }.status shouldBe HttpStatusCode.InternalServerError
            client.get("/uten-ident") { bearerAuth(token) }.bodyAsText() shouldBe "OK"
        }

    @Test
    fun `samtidige requester beholder hver sin brukerTokenInfo`() =
        testApplication {
            val foersteRequestStartet = CompletableDeferred<Unit>()
            val andreRequestStartet = CompletableDeferred<Unit>()
            runServer(oauth) {
                install(AssertPlugin)
                install(BrukerTokenInfoPlugin)
                get("/ident") {
                    val ident = requestBrukerTokenInfo.get()?.ident()
                    withTimeout(10_000.milliseconds) {
                        if (ident == "Z111111") {
                            foersteRequestStartet.complete(Unit)
                            andreRequestStartet.await()
                        } else {
                            andreRequestStartet.complete(Unit)
                            foersteRequestStartet.await()
                        }
                    }
                    withContext(Dispatchers.IO) {
                        requestBrukerTokenInfo.get()?.ident() shouldBe ident
                    }
                    coroutineScope {
                        async(Dispatchers.Default) {
                            requestBrukerTokenInfo.get()?.ident() shouldBe ident
                        }.await()
                    }
                    call.respondText(requireNotNull(requestBrukerTokenInfo.get()).ident())
                }
            }

            val foersteToken = oauth.issueSaksbehandlerToken(navIdent = "Z111111")
            val andreToken = oauth.issueSaksbehandlerToken(navIdent = "Z222222")
            startApplication()
            coroutineScope {
                val foerste = async { client.get("/ident") { bearerAuth(foersteToken) }.bodyAsText() }
                val andre = async { client.get("/ident") { bearerAuth(andreToken) }.bodyAsText() }
                foerste.await() shouldBe "Z111111"
                andre.await() shouldBe "Z222222"
            }
        }

    @Test
    fun `maskinkall bruker systemnavn og uautentiserte kall avvises`() =
        testApplication {
            runServer(oauth) {
                install(AssertPlugin)
                install(BrukerTokenInfoPlugin)
                get("/ident") {
                    val bruker = requestBrukerTokenInfo.get().shouldBeInstanceOf<VanligSystembruker>()
                    call.respondText(bruker.ident())
                }
            }

            val token = oauth.issueSystembrukerToken(mittsystem = "testsystem")
            client.get("/ident") { bearerAuth(token) }.bodyAsText() shouldBe "testsystem"
            client.get("/ident").status shouldBe HttpStatusCode.Unauthorized
            client.get("/health/isalive").status shouldBe HttpStatusCode.OK
            requestBrukerTokenInfo.get() shouldBe null
        }
}

private object AssertHook : Hook<(ApplicationCall) -> Unit> {
    override fun install(
        pipeline: ApplicationCallPipeline,
        handler: (ApplicationCall) -> Unit,
    ) {
        pipeline.intercept(ApplicationCallPipeline.Call) {
            handler(call)
            proceed()
            handler(call)
        }
    }
}

/** Sjekker at requestBrukerTokenInfo er null før og etter routen kjøres */
val AssertPlugin =
    createRouteScopedPlugin("AssertPlugin") {
        on(AssertHook) { _ ->
            requestBrukerTokenInfo.get() shouldBe null
        }
    }
