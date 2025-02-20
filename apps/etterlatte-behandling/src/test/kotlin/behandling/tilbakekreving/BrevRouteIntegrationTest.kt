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
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.brev.BrevKlient
import no.nav.etterlatte.brev.BrevPayload
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.Pdf
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.common.Enheter
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
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingResultat
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.module
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BrevRouteIntegrationTest : BehandlingIntegrationTest() {
    private lateinit var behandlingDao: BehandlingDao
    private lateinit var sakSkrivDao: SakSkrivDao
    private lateinit var tilbakekrevingDao: TilbakekrevingDao

    private val tilbakestiltPayload =
        BrevPayload(
            Slate(
                listOf(
                    Slate.Element(
                        Slate.ElementType.HEADING_TWO,
                        listOf(Slate.InnerElement(text = "Tilbakestilt!")),
                    ),
                ),
            ),
            emptyList(),
        )
    private val generertPdf = Pdf("Generert".toByteArray())

    private val brevApiKlientMock: BrevApiKlient =
        mockk {
            coEvery { opprettVedtaksbrev(any(), any<SakId>(), any()) } answers {
                opprettetBrevDto(SakId(secondArg()))
            }
            coEvery { tilbakestillVedtaksbrev(any(), any(), any(), any(), any()) } returns tilbakestiltPayload
            coEvery { genererPdf(any(), any(), any()) } returns generertPdf
        }
    private val vedtakKlient: VedtakKlient = mockk()
    private val brevKlientMock: BrevKlient =
        mockk {
            coEvery { opprettVedtaksbrev(any(), any(), any()) } returns opprettBrev()
            coEvery { tilbakestillVedtaksbrev(any(), any(), any(), any()) } returns
                mockk {
                    every { hoveddel } returns Slate()
                    every { vedlegg } returns emptyList()
                }
        }

    @BeforeEach
    fun setUp() {
        sakSkrivDao = applicationContext.sakSkrivDao
        behandlingDao = applicationContext.behandlingDao
        tilbakekrevingDao = applicationContext.tilbakekrevingDao
    }

    @BeforeAll
    fun start() {
        startServer(
            brevApiKlient = brevApiKlientMock,
            brevKlient = brevKlientMock,
            vedtakKlient = vedtakKlient,
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
            val sak = opprettSak()
            val behandling = opprettBehandling(sak)

            withTestApplication { client ->
                val response =
                    client.post("/api/behandling/brev/${behandling.id}/vedtak?sakId=${behandling.sakId}") {
                        addAuthToken(tokenSaksbehandler)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                response.status shouldBe HttpStatusCode.Created

                coVerify { brevApiKlientMock.opprettVedtaksbrev(behandling.id, behandling.sakId, any()) }
            }
        }

        @Test
        fun `skal tilbakestille vedtaksbrev`() {
            val sak = opprettSak()
            val behandling = opprettBehandling(sak)

            withTestApplication { client ->
                val response =
                    client.put(
                        "/api/behandling/brev/${behandling.id}/vedtak/tilbakestill?" +
                            "brevId=42&sakId=${behandling.sakId}&brevtype=${Brevtype.VEDTAK}",
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
                        behandling.sakId,
                        Brevtype.VEDTAK,
                        any(),
                    )
                }
            }
        }

        @Test
        fun `skal generere pdf`() {
            val sak = opprettSak()
            val behandling = opprettBehandling(sak)
            coEvery { vedtakKlient.hentVedtak(any(), any()) } returns vedtak(sak, behandling.id, behandling)

            withTestApplication { client ->
                val response =
                    client.get(
                        "/api/behandling/brev/${behandling.id}/vedtak/pdf?" +
                            "brevId=42&sakId=${behandling.sakId}",
                    ) {
                        addAuthToken(tokenSaksbehandler)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                response.status shouldBe HttpStatusCode.OK
                coVerify {
                    val pdf =
                        brevApiKlientMock.genererPdf(
                            42,
                            behandling.id,
                            any(),
                        )
                    pdf shouldBeEqual generertPdf
                }
            }
        }
    }

    @Nested
    inner class NyPushModell {
        @Test
        fun `skal opprette vedtaksbrev for tilbakekreving`() {
            val sak = opprettSak()
            val tilbakekreving = opprettTilbakekreving(sak)
            coEvery { vedtakKlient.hentVedtak(any(), any()) } returns vedtak(sak, tilbakekreving.id, tilbakekreving)

            withTestApplication { client ->
                val response =
                    client.post("/api/behandling/brev/${tilbakekreving.id}/vedtak?sakId=${sak.id}") {
                        addAuthToken(tokenSaksbehandler)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                response.status shouldBe HttpStatusCode.Created
                coVerify { brevKlientMock.opprettVedtaksbrev(tilbakekreving.id, any(), any()) }
            }
        }

        @Test
        fun `skal tilbakestille vedtaksbrev for tilbakekreving`() {
            val sak = opprettSak()
            val tilbakekreving = opprettTilbakekreving(sak)
            coEvery { vedtakKlient.hentVedtak(any(), any()) } returns vedtak(sak, tilbakekreving.id, tilbakekreving)

            withTestApplication { client ->
                val response =
                    client.put(
                        "/api/behandling/brev/${tilbakekreving.id}/vedtak/tilbakestill?" +
                            "brevId=42&sakId=${sak.id}&brevtype=${Brevtype.VEDTAK}",
                    ) {
                        addAuthToken(tokenSaksbehandler)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                response.status shouldBe HttpStatusCode.OK
                response.body<BrevPayload>() shouldBeEqual tilbakestiltPayload
                coVerify {
                    brevKlientMock.tilbakestillVedtaksbrev(
                        42,
                        tilbakekreving.id,
                        any(),
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

    private fun opprettBehandling(sak: Sak) =
        inTransaction {
            opprettBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, sak.id)
                .also { behandlingDao.opprettBehandling(it) }
        }

    private fun opprettSak() =
        inTransaction {
            sakSkrivDao.opprettSak(
                SOEKER_FOEDSELSNUMMER.value,
                SakType.OMSTILLINGSSTOENAD,
                Enheter.defaultEnhet.enhetNr,
            )
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

    private fun opprettetBrevDto(sakId: SakId) =
        Brev(
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
            behandlingId = null,
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
        vedtakInnhold: Any,
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
        innhold = VedtakInnholdDto.VedtakTilbakekrevingDto(vedtakInnhold.toObjectNode()),
    )

    private fun opprettBrev(
        status: Status = Status.OPPRETTET,
        mottakere: List<Mottaker> = listOf(opprettMottaker(SOEKER_FOEDSELSNUMMER.value)),
    ) = Brev(
        id = Random.nextLong(10000),
        sakId = randomSakId(),
        behandlingId = null,
        tittel = null,
        spraak = Spraak.NB,
        prosessType = BrevProsessType.REDIGERBAR,
        soekerFnr = "fnr",
        status = status,
        statusEndret = Tidspunkt.now(),
        opprettet = Tidspunkt.now(),
        mottakere = mottakere,
        brevtype = Brevtype.INFORMASJON,
        brevkoder = Brevkoder.TOMT_INFORMASJONSBREV,
    )

    private fun opprettMottaker(fnr: String) =
        Mottaker(
            id = UUID.randomUUID(),
            navn = "Stor Snerk",
            foedselsnummer = MottakerFoedselsnummer(fnr),
            orgnummer = null,
            adresse =
                Adresse(
                    adresseType = "NORSKPOSTADRESSE",
                    adresselinje1 = "Testgaten 13",
                    postnummer = "1234",
                    poststed = "OSLO",
                    land = "Norge",
                    landkode = "NOR",
                ),
        )
}
