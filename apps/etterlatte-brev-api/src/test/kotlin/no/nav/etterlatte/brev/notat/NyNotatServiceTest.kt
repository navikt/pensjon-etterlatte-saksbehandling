package no.nav.etterlatte.brev.notat

import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.DatabaseExtension
import no.nav.etterlatte.brev.NotatAlleredeJournalfoert
import no.nav.etterlatte.brev.NyNotatService
import no.nav.etterlatte.brev.dokarkiv.BrukerIdType
import no.nav.etterlatte.brev.dokarkiv.DokarkivService
import no.nav.etterlatte.brev.dokarkiv.OpprettJournalpost
import no.nav.etterlatte.brev.dokarkiv.OpprettJournalpostResponse
import no.nav.etterlatte.brev.dokarkiv.Sakstype
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.pdfgen.PdfGenRequest
import no.nav.etterlatte.brev.pdfgen.PdfGeneratorKlient
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class NyNotatServiceTest(
    dataSource: DataSource,
) {
    private val pdfGeneratorKlientMock = mockk<PdfGeneratorKlient>()
    private val dokarkivServiceMock = mockk<DokarkivService>()
    private val behandlingServiceMock = mockk<BehandlingService>()

    private val nyNotatService =
        NyNotatService(
            NotatRepository(dataSource),
            pdfGeneratorKlientMock,
            dokarkivServiceMock,
            behandlingServiceMock,
        )

    private val saksbehandler = simpleSaksbehandler("Z123456")

    @BeforeEach
    fun before() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified()
    }

    @Test
    fun `Hent og opprett notat for sak fungerer`() {
        val sakId = Random.nextLong()

        coEvery { behandlingServiceMock.hentSak(any(), any()) } returns Sak("ident", SakType.BARNEPENSJON, sakId, "4808")

        assertEquals(0, nyNotatService.hentForSak(sakId).size)

        val nyttNotat =
            runBlocking {
                nyNotatService.opprett(
                    sakId = sakId,
                    mal = NotatMal.TOM_MAL,
                    bruker = saksbehandler,
                )
            }

        assertEquals(sakId, nyttNotat.sakId)
        assertEquals("Mangler tittel", nyttNotat.tittel)
        assertNull(nyttNotat.journalpostId)
        assertTrue(nyttNotat.kanRedigeres())

        val notater = nyNotatService.hentForSak(sakId)
        assertEquals(1, notater.size)

        coVerify {
            behandlingServiceMock.hentSak(sakId, saksbehandler)

            pdfGeneratorKlientMock wasNot Called
            dokarkivServiceMock wasNot Called
        }
    }

    @Test
    fun `Oppdater tittel paa notat`() {
        val sakId = Random.nextLong()

        coEvery { behandlingServiceMock.hentSak(any(), any()) } returns Sak("ident", SakType.BARNEPENSJON, sakId, "4808")

        val nyttNotat =
            runBlocking {
                nyNotatService.opprett(
                    sakId = sakId,
                    mal = NotatMal.TOM_MAL,
                    bruker = saksbehandler,
                )
            }
        assertEquals("Mangler tittel", nyttNotat.tittel)

        val nyTittel = "En helt ny tittel"
        nyNotatService.oppdaterTittel(nyttNotat.id, nyTittel, saksbehandler)

        val oppdatertNotat = nyNotatService.hent(nyttNotat.id)

        assertEquals(nyTittel, oppdatertNotat.tittel)

        coVerify {
            behandlingServiceMock.hentSak(sakId, saksbehandler)

            pdfGeneratorKlientMock wasNot Called
            dokarkivServiceMock wasNot Called
        }
    }

    @Test
    fun `Journalfoer notat`() {
        val sakId = Random.nextLong()

        val sak = Sak("ident", SakType.BARNEPENSJON, sakId, "4808")
        coEvery { behandlingServiceMock.hentSak(any(), any()) } returns sak
        coEvery { dokarkivServiceMock.journalfoer(any()) } returns OpprettJournalpostResponse("123", true)
        coEvery { pdfGeneratorKlientMock.genererPdf(any(), any()) } returns "pdf".toByteArray()

        val nyttNotat =
            runBlocking {
                nyNotatService.opprett(
                    sakId = sakId,
                    mal = NotatMal.TOM_MAL,
                    bruker = saksbehandler,
                )
            }
        val payload = nyNotatService.hentPayload(nyttNotat.id)

        runBlocking { nyNotatService.journalfoer(nyttNotat.id, saksbehandler) }

        // Skal ikke kunne journalføre samme notat 2 ganger
        assertThrows<NotatAlleredeJournalfoert> {
            runBlocking { nyNotatService.journalfoer(nyttNotat.id, saksbehandler) }
        }

        val journalpostRequest = slot<OpprettJournalpost>()
        coVerify {
            behandlingServiceMock.hentSak(sakId, saksbehandler)
            dokarkivServiceMock.journalfoer(capture(journalpostRequest))
            pdfGeneratorKlientMock.genererPdf(PdfGenRequest(nyttNotat.tittel, payload.toJsonNode()), NotatMal.TOM_MAL)
        }

        with(journalpostRequest.captured) {
            assertEquals(sak.ident, this.bruker.id)
            assertEquals(BrukerIdType.FNR, this.bruker.idType)
            assertEquals("$sakId.${nyttNotat.id}", this.eksternReferanseId)
            assertEquals(sak.enhet, this.journalfoerendeEnhet)
            assertEquals(sak.sakType.tema, this.tema)
            assertEquals(nyttNotat.tittel, this.tittel)

            assertEquals(nyttNotat.sakId.toString(), this.sak.fagsakId)
            assertEquals(sak.sakType.tema, this.sak.tema)
            assertEquals(Sakstype.FAGSAK, this.sak.sakstype)
            assertEquals(Fagsaksystem.EY.navn, this.sak.fagsaksystem)
        }
    }
}
