package tilgangsstyring

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.lagContext
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.KLAGEID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.OPPGAVEID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.sak.SakMedGraderingOgSkjermet
import no.nav.etterlatte.sak.SakTilgangDao
import no.nav.etterlatte.tilgangsstyring.kunSaksbehandlerMedSkrivetilgang
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID

class TilgangsstyringTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("testdata")
    fun `sjekk for skrivetilgang`(
        beskrivelse: String,
        route: String,
        user: SaksbehandlerMedEnheterOgRoller,
        sakTilgangDao: SakTilgangDao,
        forventetStatus: Int,
    ) {
        withTestApplication(user, sakTilgangDao) { client ->
            val response =
                client.get(route) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                }

            Assertions.assertEquals(forventetStatus, response.status.value)
        }
    }

    companion object {
        private val mockOAuth2Server = MockOAuth2Server()

        @BeforeAll
        @JvmStatic
        fun before() {
            mockOAuth2Server.startRandomPort()
        }

        @AfterAll
        @JvmStatic
        fun after() {
            mockOAuth2Server.shutdown()
        }

        @JvmStatic
        fun testdata() =
            listOf(
                Arguments.of(
                    "Sak med tilgang",
                    "/api/sak/1",
                    mockk<SaksbehandlerMedEnheterOgRoller> {
                        every { enheterMedSkrivetilgang() } returns listOf(Enheter.PORSGRUNN.enhetNr)
                    },
                    mockk<SakTilgangDao> {
                        every {
                            hentSakMedGraderingOgSkjerming(any())
                        } returns SakMedGraderingOgSkjermet(1, null, null, Enheter.PORSGRUNN.enhetNr)
                    },
                    200,
                ),
                Arguments.of(
                    "Sak uten tilgang",
                    "/api/sak/1",
                    mockk<SaksbehandlerMedEnheterOgRoller> {
                        every { enheterMedSkrivetilgang() } returns listOf(Enheter.STEINKJER.enhetNr)
                    },
                    mockk<SakTilgangDao> {
                        every {
                            hentSakMedGraderingOgSkjerming(any())
                        } returns SakMedGraderingOgSkjermet(1, null, null, Enheter.PORSGRUNN.enhetNr)
                    },
                    403,
                ),
                Arguments.of(
                    "Behandling med tilgang",
                    "/api/behandling/${UUID.randomUUID()}",
                    mockk<SaksbehandlerMedEnheterOgRoller> {
                        every { enheterMedSkrivetilgang() } returns listOf(Enheter.PORSGRUNN.enhetNr)
                    },
                    mockk<SakTilgangDao> {
                        every {
                            hentSakMedGraderingOgSkjerming(any())
                        } returns null
                        every {
                            hentSakMedGraderingOgSkjermingPaaBehandling(any())
                        } returns SakMedGraderingOgSkjermet(1, null, null, Enheter.PORSGRUNN.enhetNr)
                    },
                    200,
                ),
                Arguments.of(
                    "Behandling uten tilgang",
                    "/api/behandling/${UUID.randomUUID()}",
                    mockk<SaksbehandlerMedEnheterOgRoller> {
                        every { enheterMedSkrivetilgang() } returns listOf(Enheter.STEINKJER.enhetNr)
                    },
                    mockk<SakTilgangDao> {
                        every {
                            hentSakMedGraderingOgSkjerming(any())
                        } returns null
                        every {
                            hentSakMedGraderingOgSkjermingPaaBehandling(any())
                        } returns SakMedGraderingOgSkjermet(1, null, null, Enheter.PORSGRUNN.enhetNr)
                    },
                    403,
                ),
                Arguments.of(
                    "Oppgave med tilgang",
                    "/api/oppgave/${UUID.randomUUID()}",
                    mockk<SaksbehandlerMedEnheterOgRoller> {
                        every { enheterMedSkrivetilgang() } returns listOf(Enheter.PORSGRUNN.enhetNr)
                    },
                    mockk<SakTilgangDao> {
                        every {
                            hentSakMedGraderingOgSkjerming(any())
                        } returns null
                        every {
                            hentSakMedGraderingOgSkjermingPaaBehandling(any())
                        } returns null
                        every {
                            hentSakMedGraderingOgSkjermingPaaOppgave(any())
                        } returns SakMedGraderingOgSkjermet(1, null, null, Enheter.PORSGRUNN.enhetNr)
                    },
                    200,
                ),
                Arguments.of(
                    "Oppgave uten tilgang",
                    "/api/oppgave/${UUID.randomUUID()}",
                    mockk<SaksbehandlerMedEnheterOgRoller> {
                        every { enheterMedSkrivetilgang() } returns listOf(Enheter.STEINKJER.enhetNr)
                    },
                    mockk<SakTilgangDao> {
                        every {
                            hentSakMedGraderingOgSkjerming(any())
                        } returns null
                        every {
                            hentSakMedGraderingOgSkjermingPaaBehandling(any())
                        } returns null
                        every {
                            hentSakMedGraderingOgSkjermingPaaOppgave(any())
                        } returns SakMedGraderingOgSkjermet(1, null, null, Enheter.PORSGRUNN.enhetNr)
                    },
                    403,
                ),
                Arguments.of(
                    "Klage med tilgang",
                    "/api/klage/${UUID.randomUUID()}",
                    mockk<SaksbehandlerMedEnheterOgRoller> {
                        every { enheterMedSkrivetilgang() } returns listOf(Enheter.PORSGRUNN.enhetNr)
                    },
                    mockk<SakTilgangDao> {
                        every {
                            hentSakMedGraderingOgSkjerming(any())
                        } returns null
                        every {
                            hentSakMedGraderingOgSkjermingPaaBehandling(any())
                        } returns null
                        every {
                            hentSakMedGraderingOgSkjermingPaaOppgave(any())
                        } returns null
                        every {
                            hentSakMedGraderingOgSkjermingPaaKlage(any())
                        } returns SakMedGraderingOgSkjermet(1, null, null, Enheter.PORSGRUNN.enhetNr)
                    },
                    200,
                ),
                Arguments.of(
                    "Klage uten tilgang",
                    "/api/klage/${UUID.randomUUID()}",
                    mockk<SaksbehandlerMedEnheterOgRoller> {
                        every { enheterMedSkrivetilgang() } returns listOf(Enheter.STEINKJER.enhetNr)
                    },
                    mockk<SakTilgangDao> {
                        every {
                            hentSakMedGraderingOgSkjerming(any())
                        } returns null
                        every {
                            hentSakMedGraderingOgSkjermingPaaBehandling(any())
                        } returns null
                        every {
                            hentSakMedGraderingOgSkjermingPaaOppgave(any())
                        } returns null
                        every {
                            hentSakMedGraderingOgSkjermingPaaKlage(any())
                        } returns SakMedGraderingOgSkjermet(1, null, null, Enheter.PORSGRUNN.enhetNr)
                    },
                    403,
                ),
                Arguments.of(
                    "Overstyrt sak med tilgang",
                    "/api/annet/sak",
                    mockk<SaksbehandlerMedEnheterOgRoller> {
                        every { enheterMedSkrivetilgang() } returns listOf(Enheter.PORSGRUNN.enhetNr)
                    },
                    mockk<SakTilgangDao> {
                        every {
                            hentSakMedGraderingOgSkjerming(2L)
                        } returns SakMedGraderingOgSkjermet(2, null, null, Enheter.PORSGRUNN.enhetNr)
                    },
                    200,
                ),
                Arguments.of(
                    "Overstyrt sak uten tilgang",
                    "/api/annet/sak",
                    mockk<SaksbehandlerMedEnheterOgRoller> {
                        every { enheterMedSkrivetilgang() } returns listOf(Enheter.STEINKJER.enhetNr)
                    },
                    mockk<SakTilgangDao> {
                        every {
                            hentSakMedGraderingOgSkjerming(2L)
                        } returns SakMedGraderingOgSkjermet(2, null, null, Enheter.PORSGRUNN.enhetNr)
                    },
                    403,
                ),
                Arguments.of(
                    "Overstyrt enhet med tilgang",
                    "/api/annet/enhet",
                    mockk<SaksbehandlerMedEnheterOgRoller> {
                        every { enheterMedSkrivetilgang() } returns listOf(Enheter.PORSGRUNN.enhetNr)
                    },
                    mockk<SakTilgangDao>(),
                    200,
                ),
                Arguments.of(
                    "Overstyrt enhet uten tilgang",
                    "/api/annet/enhet",
                    mockk<SaksbehandlerMedEnheterOgRoller> {
                        every { enheterMedSkrivetilgang() } returns listOf(Enheter.STEINKJER.enhetNr)
                    },
                    mockk<SakTilgangDao>(),
                    403,
                ),
            )
    }

    private fun withTestApplication(
        user: SaksbehandlerMedEnheterOgRoller,
        sakTilgangDao: SakTilgangDao,
        block: suspend (client: HttpClient) -> Unit,
    ) {
        testApplication {
            val client =
                runServer(mockOAuth2Server) {
                    intercept(ApplicationCallPipeline.Call) {
                        val context = lagContext(user, sakTilgangDao = sakTilgangDao)

                        withContext(
                            Dispatchers.Default +
                                Kontekst.asContextElement(
                                    value = context,
                                ),
                        ) {
                            proceed()
                        }
                        Kontekst.remove()
                    }

                    route("/api") {
                        route("/sak") {
                            route("/{$SAKID_CALL_PARAMETER}") {
                                get {
                                    kunSaksbehandlerMedSkrivetilgang {
                                        call.respond("OK")
                                    }
                                }
                            }
                        }
                        route("/behandling") {
                            route("/{$BEHANDLINGID_CALL_PARAMETER}") {
                                get {
                                    kunSaksbehandlerMedSkrivetilgang {
                                        call.respond("OK")
                                    }
                                }
                            }
                        }
                        route("/oppgave") {
                            route("/{$OPPGAVEID_CALL_PARAMETER}") {
                                get {
                                    kunSaksbehandlerMedSkrivetilgang {
                                        call.respond("OK")
                                    }
                                }
                            }
                        }
                        route("/klage") {
                            route("/{$KLAGEID_CALL_PARAMETER}") {
                                get {
                                    kunSaksbehandlerMedSkrivetilgang {
                                        call.respond("OK")
                                    }
                                }
                            }
                        }
                        route("/annet") {
                            get("/sak") {
                                kunSaksbehandlerMedSkrivetilgang(sakId = 2L) {
                                    call.respond("OK")
                                }
                            }
                            get("/enhet") {
                                kunSaksbehandlerMedSkrivetilgang(enhetNr = Enheter.PORSGRUNN.enhetNr) {
                                    call.respond("OK")
                                }
                            }
                        }
                    }
                }

            block(client)
        }
    }

    private val token: String by lazy { mockOAuth2Server.issueSaksbehandlerToken() }
}
