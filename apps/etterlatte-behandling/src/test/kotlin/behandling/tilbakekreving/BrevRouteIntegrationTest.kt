package no.nav.etterlatte.behandling.tilbakekreving

import behandling.tilbakekreving.kravgrunnlag
import behandling.tilbakekreving.tilbakekrevingVurdering
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.brev.BrevKlient
import no.nav.etterlatte.brev.BrevPayload
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.ktor.runServerWithModule
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekreving
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingResultat
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.module
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BrevRouteIntegrationTest : BehandlingIntegrationTest() {
    private lateinit var behandlingDao: BehandlingDao
    private lateinit var sakSkrivDao: SakSkrivDao
    private lateinit var tilbakekrevingDao: TilbakekrevingDao
    private lateinit var opplysningDao: OpplysningDao

    private val tilbakestiltPayload =
        BrevPayload(
            Slate(
                listOf(
                    Slate.Element(Slate.ElementType.HEADING_TWO, listOf(Slate.InnerElement(text = "Tilbakestilt!"))),
                ),
            ),
            emptyList(),
        )
    private val generertPdf = Pdf("Generert".toByteArray())

    private val brevApiKlientMock: BrevApiKlient =
        mockk {
            coEvery { opprettVedtaksbrev(any(), any<SakId>(), any()) } answers {
                opprettetBrev(behandlingId = firstArg(), sakId = SakId(secondArg()))
            }
            coEvery { tilbakestillVedtaksbrev(any(), any(), any(), any(), any()) } returns tilbakestiltPayload
            coEvery { genererPdf(any(), any(), any()) } returns generertPdf
            coEvery { ferdigstillVedtaksbrev(any(), any()) } just runs
            coEvery { hentVedtaksbrev(any(), any()) } answers {
                opprettetBrev(behandlingId = firstArg())
            }
        }
    private val vedtakKlient: VedtakKlient = mockk()
    private val grunnlagServiceMock: GrunnlagService = mockk()
    private val brevKlientMock: BrevKlient =
        mockk {
            coEvery { opprettStrukturertBrev(any(), any(), any()) } answers {
                opprettetBrev(behandlingId = firstArg())
            }
            coEvery { tilbakestillStrukturertBrev(any(), any(), any(), any()) } returns tilbakestiltPayload
            coEvery { ferdigstillStrukturertBrev(any(), any(), any()) } just runs
            coEvery { genererPdf(any(), any(), any(), any()) } returns generertPdf
        }

    @BeforeEach
    fun setUp() {
        sakSkrivDao = applicationContext.sakSkrivDao
        behandlingDao = applicationContext.behandlingDao
        tilbakekrevingDao = applicationContext.tilbakekrevingDao
        opplysningDao = applicationContext.opplysningDao
    }

    @BeforeAll
    fun start() {
        startServer(
            brevApiKlient = brevApiKlientMock,
            brevKlient = brevKlientMock,
            vedtakKlient = vedtakKlient,
            grunnlagService = grunnlagServiceMock,
        )
        val user = mockk<SaksbehandlerMedEnheterOgRoller>(relaxed = true)
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
    fun afterAllTests() {
        afterAll()
    }

    @AfterEach
    fun afterEachTest() {
        resetDatabase()
    }

    @Nested
    inner class GammelPullModell {
        @Test
        fun `skal opprette vedtaksbrev for behandling`() {
            val sak = opprettSakMedGrunnlag()
            val behandling: no.nav.etterlatte.behandling.domain.Behandling = opprettBehandling(sak)
            coEvery { vedtakKlient.hentVedtak(any(), any()) } returns
                vedtak(sak, behandling.id, vedtakBehandlingDto(behandling))

            withTestApplication { client ->
                val response =
                    client.post("/api/behandling/brev/${behandling.id}?sakId=${behandling.sak.id}") {
                        addAuthToken(tokenSaksbehandler)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                response.status shouldBe HttpStatusCode.Created

                coVerify { brevApiKlientMock.opprettVedtaksbrev(behandling.id, behandling.sak.id, any()) }
            }
        }

        @Test
        fun `skal tilbakestille vedtaksbrev`() {
            val sak = opprettSakMedGrunnlag()
            val behandling = opprettBehandling(sak)
            coEvery { vedtakKlient.hentVedtak(any(), any()) } returns vedtak(sak, behandling.id, vedtakBehandlingDto(behandling))

            withTestApplication { client ->
                val response =
                    client.put(
                        "/api/behandling/brev/${behandling.id}/tilbakestill?" +
                            "brevId=42&sakId=${behandling.sak.id}&brevtype=${Brevtype.VEDTAK}",
                    ) {
                        addAuthToken(tokenSaksbehandler)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                response.status shouldBe HttpStatusCode.OK
                response.body<BrevPayload>() shouldBeEqual tilbakestiltPayload

                coVerify {
                    brevApiKlientMock.tilbakestillVedtaksbrev(
                        42,
                        behandling.id,
                        behandling.sak.id,
                        Brevtype.VEDTAK,
                        any(),
                    )
                }
            }
        }

        @Test
        fun `skal generere pdf`() {
            val sak = opprettSakMedGrunnlag()
            val behandling = opprettBehandling(sak)
            coEvery { vedtakKlient.hentVedtak(any(), any()) } returns vedtak(sak, behandling.id, vedtakBehandlingDto(behandling))
            coEvery { brevApiKlientMock.hentBrev(any(), any(), any()) } returns
                mockk {
                    every { kanEndres() } returns true
                }

            withTestApplication { client ->
                val response =
                    client.get(
                        "/api/behandling/brev/${behandling.id}/pdf?" +
                            "brevId=42&sakId=${behandling.sak.id}",
                    ) {
                        addAuthToken(tokenSaksbehandler)
                    }
                response.status shouldBe HttpStatusCode.OK
                assertArrayEquals(generertPdf.bytes, response.body())
                coVerify { brevApiKlientMock.genererPdf(42, behandling.id, any()) }
            }
        }

        @Test
        fun `skal ferdigstille vedtaksbrev`() {
            val sak = opprettSakMedGrunnlag()
            val behandling = opprettBehandling(sak)
            coEvery { vedtakKlient.hentVedtak(any(), any()) } returns vedtak(sak, behandling.id, vedtakBehandlingDto(behandling))

            withTestApplication { client ->
                val response =
                    client.post(
                        "/api/behandling/brev/${behandling.id}/ferdigstill",
                    ) {
                        addAuthToken(tokenSaksbehandler)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                response.status shouldBe HttpStatusCode.OK

                coVerify { brevApiKlientMock.ferdigstillVedtaksbrev(behandling.id, any()) }
            }
        }

        @Test
        fun `skal hente vedtaksbrev`() {
            val sak = opprettSakMedGrunnlag()
            val behandling = opprettBehandling(sak)
            coEvery { vedtakKlient.hentVedtak(any(), any()) } returns vedtak(sak, behandling.id, vedtakBehandlingDto(behandling))

            withTestApplication { client ->
                val response =
                    client.get(
                        "/api/behandling/brev/${behandling.id}",
                    ) {
                        addAuthToken(tokenSaksbehandler)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                response.status shouldBe HttpStatusCode.OK
                val body: Brev = response.body()
                body.behandlingId shouldBe behandling.id

                coVerify { brevApiKlientMock.hentVedtaksbrev(behandling.id, any()) }
            }
        }
    }

    @Nested
    inner class NyPushModell {
        @Test
        fun `skal opprette vedtaksbrev for tilbakekreving`() {
            val sak = opprettSakMedGrunnlag()
            val tilbakekrevingBehandling = opprettTilbakekreving(sak)
            val tilbakekrevingId = tilbakekrevingBehandling.id
            val vedtakInnhold = vedtakTilbakekrevingBehandlingDto(tilbakekrevingBehandling.tilbakekreving)
            coEvery { vedtakKlient.hentVedtak(any(), any()) } returns vedtak(sak, tilbakekrevingId, vedtakInnhold)

            withTestApplication { client ->
                val response =
                    client.post("/api/behandling/brev/$tilbakekrevingId?sakId=${sak.id}") {
                        addAuthToken(tokenSaksbehandler)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                response.status shouldBe HttpStatusCode.Created
                val brev: Brev = response.body()
                brev.behandlingId shouldBe tilbakekrevingId

                coVerify { brevKlientMock.opprettStrukturertBrev(tilbakekrevingId, any(), any()) }
            }
        }

        @Test
        fun `skal tilbakestille vedtaksbrev for tilbakekreving`() {
            val sak = opprettSakMedGrunnlag()
            val tilbakekrevingBehandling = opprettTilbakekreving(sak)
            val tilbakekrevingId = tilbakekrevingBehandling.id
            val vedtakInnhold = vedtakTilbakekrevingBehandlingDto(tilbakekrevingBehandling.tilbakekreving)

            coEvery { vedtakKlient.hentVedtak(any(), any()) } returns vedtak(sak, tilbakekrevingId, vedtakInnhold)

            withTestApplication { client ->
                val response =
                    client.put(
                        "/api/behandling/brev/$tilbakekrevingId/tilbakestill?" +
                            "brevId=42&sakId=${sak.id}&brevtype=${Brevtype.VEDTAK}",
                    ) {
                        addAuthToken(tokenSaksbehandler)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                response.status shouldBe HttpStatusCode.OK
                response.body<BrevPayload>() shouldBeEqual tilbakestiltPayload
                coVerify {
                    brevKlientMock.tilbakestillStrukturertBrev(
                        42,
                        tilbakekrevingId,
                        any(),
                        any(),
                    )
                }
            }
        }

        @Test
        fun `skal generere pdf for tilbakekreving`() {
            val sak = opprettSakMedGrunnlag()
            val tilbakekrevingBehandling = opprettTilbakekreving(sak)
            val tilbakekrevingId = tilbakekrevingBehandling.id
            val vedtakInnhold = vedtakTilbakekrevingBehandlingDto(tilbakekrevingBehandling.tilbakekreving)

            coEvery { vedtakKlient.hentVedtak(any(), any()) } returns vedtak(sak, tilbakekrevingId, vedtakInnhold)
            coEvery { brevApiKlientMock.hentBrev(any(), any(), any()) } returns
                mockk {
                    every { kanEndres() } returns true
                }

            withTestApplication { client ->
                val response =
                    client.get(
                        "/api/behandling/brev/$tilbakekrevingId/pdf?" +
                            "brevId=42&sakId=${tilbakekrevingBehandling.sak.id}",
                    ) {
                        addAuthToken(tokenSaksbehandler)
                    }
                response.status shouldBe HttpStatusCode.OK
                assertArrayEquals(generertPdf.bytes, response.body())
                coVerify { brevKlientMock.genererPdf(42, tilbakekrevingId, any(), any()) }
            }
        }

        @Test
        fun `skal ferdigstille vedtaksbrev for tilbakekreving`() {
            val sak = opprettSakMedGrunnlag()
            val tilbakekrevingBehandling = opprettTilbakekreving(sak)
            val tilbakekrevingId = tilbakekrevingBehandling.id
            val vedtakInnhold = vedtakTilbakekrevingBehandlingDto(tilbakekrevingBehandling.tilbakekreving)
            coEvery { vedtakKlient.hentVedtak(any(), any()) } returns vedtak(sak, tilbakekrevingId, vedtakInnhold)

            withTestApplication { client ->
                val response =
                    client.post(
                        "/api/behandling/brev/$tilbakekrevingId/ferdigstill",
                    ) {
                        addAuthToken(tokenSaksbehandler)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldBeEqual ""
                coVerify {
                    brevKlientMock.ferdigstillStrukturertBrev(
                        tilbakekrevingId,
                        Brevtype.VEDTAK,
                        any(),
                    )
                }
            }
        }

        @Test
        fun `skal hente vedtaksbrev for tilbakekreving`() {
            val sak = opprettSakMedGrunnlag()
            val tilbakekrevingBehandling = opprettTilbakekreving(sak)
            val tilbakekrevingId = tilbakekrevingBehandling.id
            val vedtakInnhold = vedtakTilbakekrevingBehandlingDto(tilbakekrevingBehandling.tilbakekreving)
            coEvery { vedtakKlient.hentVedtak(any(), any()) } returns vedtak(sak, tilbakekrevingId, vedtakInnhold)

            withTestApplication { client ->
                val response =
                    client.get(
                        "/api/behandling/brev/$tilbakekrevingId",
                    ) {
                        addAuthToken(tokenSaksbehandler)
                    }
                response.status shouldBe HttpStatusCode.OK
                val body: Brev = response.body()
                body.behandlingId shouldBe tilbakekrevingId

                coVerify {
                    brevApiKlientMock.hentVedtaksbrev(
                        tilbakekrevingId,
                        any(),
                    )
                }
            }
        }
    }

    private fun opprettTilbakekreving(sak: Sak): TilbakekrevingBehandling {
        val tilbakekreving =
            inTransaction {
                tilbakekrevingDao.lagreTilbakekreving(
                    TilbakekrevingBehandling
                        .ny(
                            sak = sak,
                            kravgrunnlag = kravgrunnlag(sak),
                            omgjoeringAvId = null,
                        ).let {
                            it.copy(
                                tilbakekreving =
                                    it.tilbakekreving.copy(
                                        vurdering = tilbakekrevingVurdering(),
                                        perioder =
                                            it.tilbakekreving.perioder
                                                .map { per ->
                                                    per.copy(
                                                        tilbakekrevingsbeloep =
                                                            per.tilbakekrevingsbeloep.map { bel ->
                                                                bel.copy(resultat = TilbakekrevingResultat.FULL_TILBAKEKREV)
                                                            },
                                                    )
                                                },
                                    ),
                            )
                        },
                )
            }
        return tilbakekreving
    }

    private fun opprettBehandling(sak: Sak): Foerstegangsbehandling =
        inTransaction {
            val opprettBehandling = opprettBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, sak.id)
            behandlingDao.opprettBehandling(opprettBehandling)
            behandlingDao.hentBehandling(opprettBehandling.id) as Foerstegangsbehandling
        }

    private fun opprettSakMedGrunnlag() =
        inTransaction {
            val sak =
                sakSkrivDao.opprettSak(
                    SOEKER_FOEDSELSNUMMER.value,
                    SakType.OMSTILLINGSSTOENAD,
                    Enheter.defaultEnhet.enhetNr,
                )
            every { grunnlagServiceMock.hentOpplysningsgrunnlagForSak(sak.id) } returns
                GrunnlagTestData().hentOpplysningsgrunnlag()
            sak
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

    private fun opprettetBrev(
        sakId: SakId = randomSakId(),
        behandlingId: UUID = UUID.randomUUID(),
    ) = Brev(
        id = 1L,
        status = Status.OPPRETTET,
        mottakere =
            listOf(
                Mottaker(
                    UUID.randomUUID(),
                    navn = "Mottaker mottakersen",
                    foedselsnummer = MottakerFoedselsnummer("19448310410"),
                    orgnummer = null,
                    adresse =
                        Adresse(
                            adresseType = "",
                            landkode = "",
                            land = "",
                        ),
                    journalpostId = null,
                    bestillingId = null,
                ),
            ),
        sakId = sakId,
        behandlingId = behandlingId,
        tittel = null,
        spraak = Spraak.NB,
        prosessType = BrevProsessType.REDIGERBAR,
        soekerFnr = "",
        statusEndret = Tidspunkt.now(),
        opprettet = Tidspunkt.now(),
        brevtype = Brevtype.MANUELT,
        brevkoder = Brevkoder.TOMT_INFORMASJONSBREV,
    )

    private fun vedtak(
        sak: Sak,
        behandlingId: UUID,
        vedtakInnhold: VedtakInnholdDto,
        vedtakId: Long = 1,
        ident: String = "12345678913",
    ) = VedtakDto(
        id = vedtakId,
        behandlingId = behandlingId,
        status = VedtakStatus.FATTET_VEDTAK,
        sak =
            VedtakSak(
                id = sak.id,
                ident = ident,
                sakType = sak.sakType,
            ),
        type = VedtakType.INNVILGELSE,
        vedtakFattet =
            VedtakFattet(
                ansvarligSaksbehandler = "12345678",
                ansvarligEnhet = Enhetsnummer("1234"),
                tidspunkt = Tidspunkt.now(),
            ),
        attestasjon =
            Attestasjon(
                attestant = "87654321",
                attesterendeEnhet = Enhetsnummer("1234"),
                tidspunkt = Tidspunkt.now(),
            ),
        innhold = vedtakInnhold,
    )

    private fun vedtakBehandlingDto(behandling: no.nav.etterlatte.behandling.domain.Behandling) =
        VedtakInnholdDto.VedtakBehandlingDto(
            behandling = Behandling(behandling.type, behandling.id, behandling.revurderingsaarsak()),
            virkningstidspunkt = YearMonth.of(2022, 1),
            utbetalingsperioder =
                emptyList(),
            opphoerFraOgMed = null,
        )

    private fun vedtakTilbakekrevingBehandlingDto(tilbakekreving: Tilbakekreving) =
        VedtakInnholdDto.VedtakTilbakekrevingDto(
            tilbakekreving.toObjectNode(),
        )
}
