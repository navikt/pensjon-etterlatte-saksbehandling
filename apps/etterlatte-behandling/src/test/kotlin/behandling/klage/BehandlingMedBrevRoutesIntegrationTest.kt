package no.nav.etterlatte.behandling.klage

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BehandlingFactory
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.defaultPersongalleriGydligeFnr
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.ktor.runServerWithModule
import no.nav.etterlatte.libs.common.UUID30
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tilbakekreving.Kontrollfelt
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagId
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagStatus
import no.nav.etterlatte.libs.common.tilbakekreving.NavIdent
import no.nav.etterlatte.libs.common.tilbakekreving.SakId
import no.nav.etterlatte.libs.common.tilbakekreving.VedtakId
import no.nav.etterlatte.libs.testdata.grunnlag.BARN_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.HALVSOESKEN_ANNEN_FORELDER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.module
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BehandlingMedBrevRoutesIntegrationTest : BehandlingIntegrationTest() {
    private lateinit var user: SaksbehandlerMedEnheterOgRoller

    @BeforeAll
    fun beforeAll() {
        startServer(
            featureToggleService = DummyFeatureToggleService(),
        ).also {
            resetDatabase()
        }
        user = mockk<SaksbehandlerMedEnheterOgRoller>(relaxed = true)
        val saksbehandlerMedRoller =
            mockk<SaksbehandlerMedRoller> {
                every { harRolleStrengtFortrolig() } returns false
                every { harRolleEgenAnsatt() } returns false
            }
        every { user.saksbehandlerMedRoller } returns saksbehandlerMedRoller
        every { user.name() } returns "User"
        every { user.enheter() } returns listOf(Enheter.defaultEnhet.enhetNr)

        nyKontekstMedBrukerOgDatabase(user, applicationContext.dataSource)
    }

    @AfterAll
    fun shutdown() = afterAll()

    @Test
    fun `henting av redigerbar gir true for klage med redigerbar status`() {
        val (sak, _) =
            opprettSakMedFoerstegangsbehandling(
                SOEKER_FOEDSELSNUMMER.value,
                applicationContext.behandlingFactory,
            )
        withTestApplication { client ->

            val klage: Klage = opprettKlage(client, sak)
            val hentetKlage = hentKlage(client, klage.id)
            assertEquals(klage, hentetKlage)

            val response =
                client.get("/behandling-med-brev/${klage.id}/redigerbar") {
                    addAuthToken(systemBruker)
                    contentType(ContentType.Application.Json)
                }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(
                response
                    .body<String>()
                    .let { deserialize<Boolean>(it) },
            )
        }
    }

    @Test
    fun `henting av redigerbar gir true for behandling med redigerbar status`() {
        val (_, behandling) =
            opprettSakMedFoerstegangsbehandling(
                SOEKER2_FOEDSELSNUMMER.value,
                applicationContext.behandlingFactory,
            )
        withTestApplication { client ->

            val response =
                client.get("/behandling-med-brev/${behandling!!.id}/redigerbar") {
                    addAuthToken(systemBruker)
                    contentType(ContentType.Application.Json)
                }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(
                response
                    .body<String>()
                    .let { deserialize<Boolean>(it) },
            )
        }
    }

    @Test
    fun `henting av redigerbar gir true for tilbakekreving med redigerbar status`() {
        val (sak, _) =
            opprettSakMedFoerstegangsbehandling(
                BARN_FOEDSELSNUMMER.value,
                applicationContext.behandlingFactory,
            )

        val tilbakekreving =
            applicationContext.tilbakekrevingService.opprettTilbakekreving(
                kravgrunnlag(sak),
                null,
            )
        withTestApplication { client ->
            val response =
                client.get("/behandling-med-brev/${tilbakekreving.id}/redigerbar") {
                    addAuthToken(systemBruker)
                    contentType(ContentType.Application.Json)
                }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(
                response
                    .body<String>()
                    .let { deserialize<Boolean>(it) },
            )
        }
    }

    @Test
    fun `henting av redigerbar gir 404 naar ingen behandling med gitt id finnes`() {
        opprettSakMedFoerstegangsbehandling(HALVSOESKEN_ANNEN_FORELDER.value)

        withTestApplication { client ->
            val response =
                client.get("/behandling-med-brev/${UUID.randomUUID()}/redigerbar") {
                    addAuthToken(tokenSaksbehandler)
                    contentType(ContentType.Application.Json)
                }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    private suspend fun hentKlage(
        client: HttpClient,
        klageId: UUID,
    ): Klage {
        val response =
            client.get("/api/klage/$klageId") {
                addAuthToken(tokenSaksbehandler)
            }
        assertEquals(HttpStatusCode.OK, response.status)
        val hentetKlage = response.body<Klage>()
        return hentetKlage
    }

    private fun opprettSakMedFoerstegangsbehandling(
        fnr: String,
        behandlingFactory: BehandlingFactory? = null,
    ): Pair<Sak, Foerstegangsbehandling?> {
        val sak =
            inTransaction {
                applicationContext.sakSkrivDao.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr)
            }
        val factory = behandlingFactory ?: applicationContext.behandlingFactory
        val behandling =
            inTransaction {
                factory
                    .opprettBehandling(
                        sak.id,
                        defaultPersongalleriGydligeFnr,
                        LocalDateTime.now().toString(),
                        Vedtaksloesning.GJENNY,
                        factory.hentDataForOpprettBehandling(sak.id),
                        BehandlingOpprinnelse.UKJENT,
                    )
            }.behandling

        return Pair(sak, behandling as Foerstegangsbehandling)
    }

    private suspend fun opprettKlage(
        client: HttpClient,
        sak: Sak,
    ): Klage {
        val klage: Klage =
            client
                .post("/api/klage/opprett/${sak.id}") {
                    addAuthToken(tokenSaksbehandler)
                    contentType(ContentType.Application.Json)
                    setBody(
                        InnkommendeKlageDto(
                            mottattDato = OffsetDateTime.now().toString(),
                            journalpostId = "",
                            innsender = "En klager",
                        ),
                    )
                }.body()
        return klage
    }

    private fun withTestApplication(block: suspend (client: HttpClient) -> Unit) {
        testApplication {
            val client =
                runServerWithModule(mockOAuth2Server) {
                    module(applicationContext)
                }
            block(client)
        }
    }

    private fun kravgrunnlag(sak: Sak) =
        Kravgrunnlag(
            kravgrunnlagId = KravgrunnlagId(123L),
            sakId = SakId(sak.id.sakId),
            vedtakId = VedtakId(2L),
            kontrollFelt = Kontrollfelt(""),
            status = KravgrunnlagStatus.ANNU,
            saksbehandler = NavIdent(""),
            referanse = UUID30(""),
            perioder = emptyList(),
        )
}
