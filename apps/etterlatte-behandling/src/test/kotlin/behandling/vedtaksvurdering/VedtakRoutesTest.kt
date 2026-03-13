package no.nav.etterlatte.behandling.vedtaksvurdering

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.defaultPersongalleriGydligeFnr
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.ktor.runServerWithModule
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.NyBehandlingRequest
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.module
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VedtakRoutesTest : BehandlingIntegrationTest() {
    private lateinit var user: SaksbehandlerMedEnheterOgRoller
    private lateinit var brukerTokenInfo: BrukerTokenInfo

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
        brukerTokenInfo = Kontekst.get().brukerTokenInfo ?: throw RuntimeException("Bruker token info not set")
    }

    @AfterAll
    fun shutdown() = afterAll()

    @Test
    fun `skal opprette vedtak`() {
        val behandling =
            applicationContext.behandlingFactory.opprettSakOgBehandlingForOppgave(
                request =
                    NyBehandlingRequest(
                        sakType = SakType.BARNEPENSJON,
                        persongalleri = defaultPersongalleriGydligeFnr,
                        mottattDato = "1999-12-31T23:59:59",
                        spraak = "NB",
                        kilde = Vedtaksloesning.GJENNY,
                        pesysId = null,
                        enhet = Enheter.defaultEnhet.enhetNr,
                    ),
                brukerTokenInfo = mockk(),
            )
        inTransaction {
            applicationContext.gyldighetsproevingService.lagreGyldighetsproeving(
                behandling.id,
                JaNeiMedBegrunnelse(JaNei.JA, ""),
                kildeSaksbehandler(),
            )
            applicationContext.kommerBarnetTilGodeDao.lagreKommerBarnetTilGode(
                KommerBarnetTilgode(JaNei.JA, "", kildeSaksbehandler(), behandling.id),
            )
            applicationContext.vilkaarsvurderingService.opprettVilkaarsvurdering(behandling.id, brukerTokenInfo)
            applicationContext.vilkaarsvurderingService.oppdaterTotalVurdering(
                behandling.id,
                brukerTokenInfo,
                VilkaarsvurderingResultat(VilkaarsvurderingUtfall.OPPFYLT, "", LocalDateTime.now(), ""),
            )
        }

        val token: String = tokenSaksbehandler
        withTestApplication { client ->
            val response =
                client.post("/api/vedtak/${behandling.id}/upsert") {
                    addAuthToken(token)
                    contentType(ContentType.Application.Json)
                }
            response.status shouldBe HttpStatusCode.Created
        }
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

    private fun kildeSaksbehandler() = Grunnlagsopplysning.Saksbehandler(ident = "ident", tidspunkt = Tidspunkt(instant = Instant.now()))
}
