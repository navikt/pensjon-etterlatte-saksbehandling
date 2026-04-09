package no.nav.etterlatte.behandling.vedtaksvurdering

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.vedtaksvurdering.outbox.OutboxItemType
import no.nav.etterlatte.behandling.vedtaksvurdering.outbox.OutboxRepository
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.defaultPersongalleriGydligeFnr
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.grunnlag.GrunnlagVersjonValidering
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.ktor.runServerWithModule
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.NyBehandlingRequest
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.vedtak.AttesterVedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.module
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VedtaksvurderingRoutesIntegrationTest : BehandlingIntegrationTest() {
    private val mottattTidspunkt = LocalDateTime.of(2024, 1, 16, 13, 0, 0)
    private val virkningstidspunkt = YearMonth.of(2024, 2)

    private lateinit var user: SaksbehandlerMedEnheterOgRoller
    private lateinit var brukerTokenInfo: BrukerTokenInfo
    private lateinit var context: Context

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

        context = nyKontekstMedBrukerOgDatabase(user, applicationContext.dataSource)
        brukerTokenInfo = context.brukerTokenInfo ?: throw RuntimeException("Bruker token info not set")

        mockkObject(GrunnlagVersjonValidering)
        every { GrunnlagVersjonValidering.validerVersjon(any(), any(), any(), any()) } just runs
    }

    @AfterAll
    fun shutdown() {
        afterAll()
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() {
        resetDatabase()
    }

    private val vedtaksvurderingService
        get() = applicationContext.vedtaksvurderingService

    @Test
    fun `skal opprette vedtak`() {
        val behandling = opprettBehandlingKlarForVedtak()

        withTestApplication { client ->
            val response =
                client.post("/api/vedtak/${behandling.id}/upsert") {
                    addAuthToken(tokenSaksbehandler)
                    contentType(ContentType.Application.Json)
                }
            response.status shouldBe HttpStatusCode.OK
            val opprettetVedtak: VedtakDto = deserialize(response.body())

            inTransaction {
                with(vedtaksvurderingService.hentVedtakMedBehandlingId(behandling.id)!!) {
                    type shouldBe VedtakType.INNVILGELSE
                    vedtakFattet shouldBe null
                    sakId shouldBe behandling.sak.id
                    id shouldBe opprettetVedtak.id
                    val vedtakInnhold = innhold as VedtakInnhold.Behandling
                    vedtakInnhold.virkningstidspunkt shouldBe virkningstidspunkt
                    vedtakInnhold.opphoerFraOgMed shouldBe null
                }
            }
        }
    }

    @Test
    fun `skal fatte vedtak`() {
        val behandling = opprettBehandlingKlarForVedtak()
        settOppgaveTilUnderBehandling(behandling)

        withTestApplication { client ->
            client.post("/api/vedtak/${behandling.id}/upsert") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
            }
            val response =
                client.post("/api/vedtak/${behandling.id}/fattvedtak") {
                    addAuthToken(tokenSaksbehandler)
                    contentType(ContentType.Application.Json)
                }
            response.status shouldBe HttpStatusCode.OK
        }
        inTransaction {
            with(vedtaksvurderingService.hentVedtakMedBehandlingId(behandling.id)!!) {
                type shouldBe VedtakType.INNVILGELSE
                vedtakFattet?.ansvarligSaksbehandler shouldBe saksbehandlerIdent
                sakId shouldBe behandling.sak.id
                val vedtakInnhold = innhold as VedtakInnhold.Behandling
                vedtakInnhold.virkningstidspunkt shouldBe virkningstidspunkt
                vedtakInnhold.opphoerFraOgMed shouldBe null
            }
        }
    }

    @Test
    fun `skal attestere vedtak`() {
        val behandling = opprettBehandlingKlarForVedtak()
        settOppgaveTilUnderBehandling(behandling)
        inTransaction {
            applicationContext.oppgaveService
                .hentOppgaverForReferanse(behandling.id.toString())
                .forEach { println(it.status) }
        }
        withTestApplication { client ->
            client.post("/api/vedtak/${behandling.id}/upsert") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
            }
            client.post("/api/vedtak/${behandling.id}/fattvedtak") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
            }

            tildelOppgave(behandling, attestantIdent)
            val response =
                client.post("/api/vedtak/${behandling.id}/attester") {
                    addAuthToken(tokenAttestant)
                    contentType(ContentType.Application.Json)
                    setBody(AttesterVedtakDto("Attestert fordi alt ser flott ut"))
                }
            response.status shouldBe HttpStatusCode.OK
        }
        inTransaction {
            val vedtak = vedtaksvurderingService.hentVedtakMedBehandlingId(behandling.id)
            with(vedtak!!) {
                type shouldBe VedtakType.INNVILGELSE
                vedtakFattet?.ansvarligSaksbehandler shouldBe saksbehandlerIdent
                attestasjon?.attestant shouldBe attestantIdent
                sakId shouldBe behandling.sak.id
                val vedtakInnhold = innhold as VedtakInnhold.Behandling
                vedtakInnhold.virkningstidspunkt shouldBe virkningstidspunkt
                vedtakInnhold.opphoerFraOgMed shouldBe null
            }
            val upublisertEksternVedtakshendelse =
                OutboxRepository(applicationContext.dataSource)
                    .hentUpubliserte()
                    .single()
            upublisertEksternVedtakshendelse.vedtakId shouldBe vedtak.id
            upublisertEksternVedtakshendelse.type shouldBe OutboxItemType.ATTESTERT
        }
    }

    private fun settOppgaveTilUnderBehandling(behandling: Behandling) {
        inTransaction {
            val oppgave =
                applicationContext.oppgaveService
                    .hentOppgaverForReferanse(
                        referanse = behandling.id.toString(),
                    ).single()
            applicationContext.oppgaveService.oppdaterStatusOgMerknad(
                status = Status.UNDER_BEHANDLING,
                oppgaveId = oppgave.id,
                merknad = "",
            )
        }
    }

    private fun tildelOppgave(
        behandling: Behandling,
        saksbehandler: String,
    ) {
        inTransaction {
            val oppgave =
                applicationContext.oppgaveService
                    .hentOppgaverForReferanse(
                        referanse = behandling.id.toString(),
                    ).single()
            applicationContext.oppgaveService.tildelSaksbehandler(
                oppgaveId = oppgave.id,
                saksbehandler = saksbehandler,
            )
        }
    }

    private fun opprettBehandlingKlarForVedtak(): Behandling {
        val behandling = opprettSakOgBehandling()
        inTransaction {
            settVirkningstidspunkt(behandling)
            lagreOkGyldighetsproeving(behandling)
            lagreOkKommerBarnetTilGode(behandling)
            lagreOkVilkaarsvurdering(behandling)
        }
        coEvery { applicationContext.beregningKlient.hentBeregning(any(), any()) } returns beregningDto(behandling)
        return behandling
    }

    private fun beregningDto(behandling: Behandling): BeregningDTO =
        BeregningDTO(
            beregningId = UUID.randomUUID(),
            behandlingId = behandling.id,
            type = Beregningstype.BP,
            beregningsperioder = emptyList(),
            beregnetDato = mottattTidspunkt.toTidspunkt(),
            grunnlagMetadata = mockk(relaxed = true),
            overstyrBeregning = null,
        )

    private fun settVirkningstidspunkt(behandling: Behandling) {
        runBlocking {
            applicationContext.behandlingService.oppdaterVirkningstidspunkt(
                behandlingId = behandling.id,
                virkningstidspunkt = virkningstidspunkt,
                brukerTokenInfo = brukerTokenInfo,
                begrunnelse = "",
                kravdato = null,
            )
        }
    }

    private fun lagreOkKommerBarnetTilGode(behandling: Behandling) {
        applicationContext.kommerBarnetTilGodeDao.lagreKommerBarnetTilGode(
            KommerBarnetTilgode(JaNei.JA, "", kildeSaksbehandler(), behandling.id),
        )
    }

    private fun lagreOkGyldighetsproeving(behandling: Behandling) {
        applicationContext.gyldighetsproevingService.lagreGyldighetsproeving(
            behandling.id,
            JaNeiMedBegrunnelse(JaNei.JA, ""),
            kildeSaksbehandler(),
        )
    }

    private fun withTestApplication(block: suspend (client: HttpClient) -> Unit) {
        testApplication(Kontekst.asContextElement(context)) {
            val client =
                runServerWithModule(mockOAuth2Server) {
                    module(applicationContext)
                }
            block(client)
        }
    }

    private fun kildeSaksbehandler() = Grunnlagsopplysning.Saksbehandler(ident = "ident", tidspunkt = Tidspunkt(instant = Instant.now()))

    private fun opprettSakOgBehandling(): Behandling =
        applicationContext.behandlingFactory.opprettSakOgBehandlingForOppgave(
            request =
                NyBehandlingRequest(
                    sakType = SakType.BARNEPENSJON,
                    persongalleri = defaultPersongalleriGydligeFnr,
                    mottattDato = mottattTidspunkt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    spraak = "NB",
                    kilde = Vedtaksloesning.GJENNY,
                    pesysId = null,
                    enhet = Enheter.defaultEnhet.enhetNr,
                ),
            brukerTokenInfo = brukerTokenInfo,
        )

    fun lagreOkVilkaarsvurdering(behandling: Behandling) {
        applicationContext.vilkaarsvurderingService.opprettVilkaarsvurdering(behandling.id, brukerTokenInfo)
        applicationContext.vilkaarsvurderingService.oppdaterTotalVurdering(
            behandling.id,
            brukerTokenInfo,
            VilkaarsvurderingResultat(VilkaarsvurderingUtfall.OPPFYLT, "", LocalDateTime.now(), ""),
        )
    }
}
