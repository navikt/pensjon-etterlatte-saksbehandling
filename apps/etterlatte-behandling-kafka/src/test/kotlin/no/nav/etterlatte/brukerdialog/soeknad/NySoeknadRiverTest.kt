package no.nav.etterlatte.fordeler

import io.ktor.client.plugins.ResponseException
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.behandling.tilSakId
import no.nav.etterlatte.brukerdialog.soeknad.NySoeknadRiver
import no.nav.etterlatte.brukerdialog.soeknad.client.BehandlingClient
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.AvsenderMottaker
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.Bruker
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.DokarkivKlient
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.JournalfoerSoeknadService
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.JournalpostSak
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.OpprettJournalpostRequest
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.OpprettJournalpostResponse
import no.nav.etterlatte.brukerdialog.soeknad.pdf.PdfGeneratorKlient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendrag
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.SakMedBehandlinger
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.FordelerFordelt
import no.nav.etterlatte.libs.common.event.GyldigSoeknadVurdert
import no.nav.etterlatte.libs.common.event.SoeknadInnsendtHendelseType
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class NySoeknadRiverTest {
    private val behandlingKlientMock = mockk<BehandlingClient>()
    private val dokarkivKlientMock = mockk<DokarkivKlient>()
    private val pdfgenKlient = mockk<PdfGeneratorKlient>()

    private val journalfoerSoeknadService = JournalfoerSoeknadService(dokarkivKlientMock, pdfgenKlient)

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
    }

    @AfterEach
    fun afterEach() {
        confirmVerified(behandlingKlientMock, dokarkivKlientMock, pdfgenKlient)
    }

    @Test
    fun `BARNEPENSJON - Skal opprette sak og journalføre søknad`() {
        val sak = Sak("25478323363", SakType.BARNEPENSJON, randomSakId(), Enheter.PORSGRUNN.enhetNr, null, null)

        coEvery { behandlingKlientMock.finnEllerOpprettSak(any(), any()) } returns sak
        coEvery { pdfgenKlient.genererPdf(any(), any()) } returns "".toByteArray()
        coEvery { dokarkivKlientMock.opprettJournalpost(any()) } returns OpprettJournalpostResponse("123", true)
        coEvery { behandlingKlientMock.hentSakMedBehandlinger(any()) } returns opprettSakMedBehandlinger()

        val inspector =
            testRapid {
                sendTestMessage(BARNEPENSJON_SOEKNAD)
            }

        val melding = inspector.message(0)
        assertEquals(
            SoeknadInnsendtHendelseType.EVENT_NAME_INNSENDT.lagEventnameForType(),
            melding.get(EVENT_NAME_KEY).asText(),
        )
        assertEquals(sak.id, melding.get(GyldigSoeknadVurdert.sakIdKey).tilSakId())
        assertTrue(melding.get(FordelerFordelt.soeknadFordeltKey).asBoolean())

        val request = slot<OpprettJournalpostRequest>()

        coVerify(exactly = 1) {
            behandlingKlientMock.finnEllerOpprettSak("25478323363", SakType.BARNEPENSJON)
            behandlingKlientMock.hentSakMedBehandlinger(sak.id)
            pdfgenKlient.genererPdf(any(), "barnepensjon_v2")
            dokarkivKlientMock.opprettJournalpost(capture(request))
        }

        with(request.captured) {
            assertEquals("Søknad om barnepensjon", this.tittel)
            assertEquals(sak.sakType.tema, this.tema)
            assertEquals(sak.enhet, this.journalfoerendeEnhet)
            assertEquals(AvsenderMottaker("25478323363"), this.avsenderMottaker)
            assertEquals(Bruker("25478323363"), this.bruker)
            assertEquals("etterlatte:barnepensjon:621", this.eksternReferanseId)
            assertEquals(JournalpostSak(sak.id.toString()), this.sak)
        }
    }

    @Test
    fun `OMSTILLINGSSTOENAD - Skal opprette sak og journalføre søknad`() {
        val sak = Sak("13848599411", SakType.OMSTILLINGSSTOENAD, randomSakId(), Enheter.PORSGRUNN.enhetNr, null, null)

        coEvery { behandlingKlientMock.finnEllerOpprettSak(any(), any()) } returns sak
        coEvery { pdfgenKlient.genererPdf(any(), any()) } returns "".toByteArray()
        coEvery { dokarkivKlientMock.opprettJournalpost(any()) } returns OpprettJournalpostResponse("123", true)
        coEvery { behandlingKlientMock.hentSakMedBehandlinger(any()) } returns opprettSakMedBehandlinger()

        val inspector =
            testRapid {
                sendTestMessage(OMSTILLINGSSTOENAD_SOEKNAD)
            }

        val melding = inspector.message(0)
        assertEquals(
            SoeknadInnsendtHendelseType.EVENT_NAME_INNSENDT.lagEventnameForType(),
            melding.get(EVENT_NAME_KEY).asText(),
        )
        assertEquals(sak.id, melding.get(GyldigSoeknadVurdert.sakIdKey).tilSakId())
        assertTrue(melding.get(FordelerFordelt.soeknadFordeltKey).asBoolean())

        val request = slot<OpprettJournalpostRequest>()

        coVerify(exactly = 1) {
            behandlingKlientMock.finnEllerOpprettSak("13848599411", SakType.OMSTILLINGSSTOENAD)
            behandlingKlientMock.hentSakMedBehandlinger(sak.id)
            pdfgenKlient.genererPdf(any(), "omstillingsstoenad_v1")
            dokarkivKlientMock.opprettJournalpost(capture(request))
        }

        with(request.captured) {
            assertEquals("Søknad om omstillingsstønad", this.tittel)
            assertEquals(sak.sakType.tema, this.tema)
            assertEquals(sak.enhet, this.journalfoerendeEnhet)
            assertEquals(AvsenderMottaker("13848599411"), this.avsenderMottaker)
            assertEquals(Bruker("13848599411"), this.bruker)
            assertEquals("etterlatte:omstillingsstoenad:42", this.eksternReferanseId)
            assertEquals(JournalpostSak(sak.id.toString()), this.sak)
        }
    }

    @Test
    fun `BARNEPENSJON - Bruker har sendt søknad nummer 2`() {
        val sak = Sak("25478323363", SakType.BARNEPENSJON, randomSakId(), Enhetsnummer("4808"), null, null)
        val journalpostId = "123"
        val journalpostResponse = OpprettJournalpostResponse(journalpostId, true)

        coEvery { behandlingKlientMock.finnEllerOpprettSak(any(), any()) } returns sak
        coEvery { pdfgenKlient.genererPdf(any(), any()) } returns "".toByteArray()
        coEvery { dokarkivKlientMock.opprettJournalpost(any()) } returns journalpostResponse
        coEvery { behandlingKlientMock.hentSakMedBehandlinger(any()) } returns
            opprettSakMedBehandlinger(
                listOf(
                    mockk<BehandlingSammendrag> { every { status } returns BehandlingStatus.OPPRETTET },
                ),
            )
        coEvery { behandlingKlientMock.opprettOppgave(any(), any()) } returns UUID.randomUUID()
        coEvery { behandlingKlientMock.finnOppgaverForReferanse(journalpostId) } returns emptyList()

        val inspector =
            testRapid {
                sendTestMessage(BARNEPENSJON_SOEKNAD)
            }

        val melding = inspector.message(0)
        assertEquals(
            SoeknadInnsendtHendelseType.EVENT_NAME_INNSENDT.lagEventnameForType(),
            melding.get(EVENT_NAME_KEY).asText(),
        )
        assertEquals(sak.id, melding.get(GyldigSoeknadVurdert.sakIdKey).tilSakId())
        assertFalse(melding.get(FordelerFordelt.soeknadFordeltKey).asBoolean(), "Ny søknad skal IKKE fordeles")

        val request = slot<OpprettJournalpostRequest>()

        coVerify(exactly = 1) {
            behandlingKlientMock.finnOppgaverForReferanse(journalpostId)
            behandlingKlientMock.finnEllerOpprettSak("25478323363", SakType.BARNEPENSJON)
            behandlingKlientMock.hentSakMedBehandlinger(sak.id)
            pdfgenKlient.genererPdf(any(), "barnepensjon_v2")
            dokarkivKlientMock.opprettJournalpost(capture(request))
            behandlingKlientMock.opprettOppgave(
                sak.id,
                NyOppgaveDto(
                    OppgaveKilde.BRUKERDIALOG,
                    OppgaveType.TILLEGGSINFORMASJON,
                    "Ny søknad mottatt mens førstegangsbehandling pågår. Kontroller innholdet og vurder konsekvens.",
                    journalpostResponse.journalpostId,
                    null,
                    null,
                ),
            )
        }

        with(request.captured) {
            assertEquals("Søknad om barnepensjon", this.tittel)
            assertEquals(sak.sakType.tema, this.tema)
            assertEquals(sak.enhet, this.journalfoerendeEnhet)
            assertEquals(AvsenderMottaker("25478323363"), this.avsenderMottaker)
            assertEquals(Bruker("25478323363"), this.bruker)
            assertEquals("etterlatte:barnepensjon:621", this.eksternReferanseId)
            assertEquals(JournalpostSak(sak.id.toString()), this.sak)
        }
    }

    fun lagNyOppgave(
        sak: Sak,
        oppgaveKilde: OppgaveKilde = OppgaveKilde.BEHANDLING,
        oppgaveType: OppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING,
        gruppeId: String? = null,
    ) = OppgaveIntern(
        id = UUID.randomUUID(),
        status = Status.NY,
        enhet = sak.enhet,
        sakId = sak.id,
        kilde = oppgaveKilde,
        referanse = "referanse",
        gruppeId = gruppeId,
        merknad = "merknad",
        opprettet = Tidspunkt.now(),
        sakType = sak.sakType,
        fnr = sak.ident,
        frist = null,
        type = oppgaveType,
    )

    @Test
    fun `BARNEPENSJON - Bruker har sendt søknad nummer 2 og nysoeknad prøver for andre gang å opprette oppgave men den finnes allerede`() {
        val ident = "25478323363"
        val sak = Sak(ident, SakType.BARNEPENSJON, randomSakId(), Enhetsnummer("4808"), null, null)
        val journalpostId = "123"
        val journalpostResponse = OpprettJournalpostResponse(journalpostId, true)

        coEvery { behandlingKlientMock.finnEllerOpprettSak(any(), any()) } returns sak
        coEvery { pdfgenKlient.genererPdf(any(), any()) } returns "".toByteArray()
        coEvery { dokarkivKlientMock.opprettJournalpost(any()) } returns journalpostResponse
        coEvery { behandlingKlientMock.hentSakMedBehandlinger(any()) } returns
            opprettSakMedBehandlinger(
                listOf(
                    mockk<BehandlingSammendrag> { every { status } returns BehandlingStatus.OPPRETTET },
                ),
            )
        coEvery { behandlingKlientMock.opprettOppgave(any(), any()) } returns UUID.randomUUID()
        coEvery { behandlingKlientMock.finnOppgaverForReferanse(journalpostId) } returns listOf(lagNyOppgave(sak))

        val inspector =
            testRapid {
                sendTestMessage(BARNEPENSJON_SOEKNAD)
            }

        val melding = inspector.message(0)
        assertEquals(
            SoeknadInnsendtHendelseType.EVENT_NAME_INNSENDT.lagEventnameForType(),
            melding.get(EVENT_NAME_KEY).asText(),
        )
        assertEquals(sak.id, melding.get(GyldigSoeknadVurdert.sakIdKey).tilSakId())
        assertFalse(melding.get(FordelerFordelt.soeknadFordeltKey).asBoolean(), "Ny søknad skal IKKE fordeles")

        val request = slot<OpprettJournalpostRequest>()

        coVerify(exactly = 1) {
            behandlingKlientMock.finnOppgaverForReferanse(journalpostId)
            behandlingKlientMock.finnEllerOpprettSak(ident, SakType.BARNEPENSJON)
            behandlingKlientMock.hentSakMedBehandlinger(sak.id)
            behandlingKlientMock.hentSakMedBehandlinger(sak.id)
            pdfgenKlient.genererPdf(any(), "barnepensjon_v2")
            dokarkivKlientMock.opprettJournalpost(capture(request))
        }

        coVerify(exactly = 0) {
            behandlingKlientMock.opprettOppgave(any(), any())
        }

        with(request.captured) {
            assertEquals("Søknad om barnepensjon", this.tittel)
            assertEquals(sak.sakType.tema, this.tema)
            assertEquals(sak.enhet, this.journalfoerendeEnhet)
            assertEquals(AvsenderMottaker(ident), this.avsenderMottaker)
            assertEquals(Bruker(ident), this.bruker)
            assertEquals("etterlatte:barnepensjon:621", this.eksternReferanseId)
            assertEquals(JournalpostSak(sak.id.toString()), this.sak)
        }
    }

    @Test
    fun `OMSTILLINGSSTOENAD - Bruker har sendt søknad nummer 2`() {
        val sak = Sak("13848599411", SakType.OMSTILLINGSSTOENAD, randomSakId(), Enhetsnummer("4808"), null, null)
        val journalpostResponse = OpprettJournalpostResponse("123", true)

        coEvery { behandlingKlientMock.finnEllerOpprettSak(any(), any()) } returns sak
        coEvery { pdfgenKlient.genererPdf(any(), any()) } returns "".toByteArray()
        coEvery { dokarkivKlientMock.opprettJournalpost(any()) } returns journalpostResponse
        coEvery { behandlingKlientMock.finnOppgaverForReferanse(any()) } returns emptyList()
        coEvery { behandlingKlientMock.hentSakMedBehandlinger(any()) } returns
            opprettSakMedBehandlinger(
                listOf(
                    mockk<BehandlingSammendrag> { every { status } returns BehandlingStatus.OPPRETTET },
                ),
            )
        coEvery { behandlingKlientMock.opprettOppgave(any(), any()) } returns UUID.randomUUID()

        val inspector =
            testRapid {
                sendTestMessage(OMSTILLINGSSTOENAD_SOEKNAD)
            }

        val melding = inspector.message(0)
        assertEquals(
            SoeknadInnsendtHendelseType.EVENT_NAME_INNSENDT.lagEventnameForType(),
            melding.get(EVENT_NAME_KEY).asText(),
        )
        assertEquals(sak.id, melding.get(GyldigSoeknadVurdert.sakIdKey).tilSakId())
        assertFalse(melding.get(FordelerFordelt.soeknadFordeltKey).asBoolean(), "Ny søknad skal IKKE fordeles")

        val request = slot<OpprettJournalpostRequest>()

        coVerify(exactly = 1) {
            behandlingKlientMock.finnOppgaverForReferanse(any())
            behandlingKlientMock.finnEllerOpprettSak("13848599411", SakType.OMSTILLINGSSTOENAD)
            behandlingKlientMock.hentSakMedBehandlinger(sak.id)
            pdfgenKlient.genererPdf(any(), "omstillingsstoenad_v1")
            dokarkivKlientMock.opprettJournalpost(capture(request))
            behandlingKlientMock.opprettOppgave(
                sak.id,
                NyOppgaveDto(
                    OppgaveKilde.BRUKERDIALOG,
                    OppgaveType.TILLEGGSINFORMASJON,
                    "Ny søknad mottatt mens førstegangsbehandling pågår. Kontroller innholdet og vurder konsekvens.",
                    journalpostResponse.journalpostId,
                    null,
                    null,
                ),
            )
        }

        with(request.captured) {
            assertEquals("Søknad om omstillingsstønad", this.tittel)
            assertEquals(sak.sakType.tema, this.tema)
            assertEquals(sak.enhet, this.journalfoerendeEnhet)
            assertEquals(AvsenderMottaker("13848599411"), this.avsenderMottaker)
            assertEquals(Bruker("13848599411"), this.bruker)
            assertEquals("etterlatte:omstillingsstoenad:42", this.eksternReferanseId)
            assertEquals(JournalpostSak(sak.id.toString()), this.sak)
        }
    }

    @Test
    fun `BARNEPENSJON - Feil ved journalføring, skal ikke sende melding`() {
        val sak = Sak("25478323363", SakType.BARNEPENSJON, randomSakId(), Enheter.PORSGRUNN.enhetNr, null, null)

        coEvery { behandlingKlientMock.finnEllerOpprettSak(any(), any()) } returns sak
        coEvery { pdfgenKlient.genererPdf(any(), any()) } returns "".toByteArray()
        coEvery { dokarkivKlientMock.opprettJournalpost(any()) } throws ResponseException(mockk(), "feil")
        coEvery { behandlingKlientMock.hentSakMedBehandlinger(any()) } returns opprettSakMedBehandlinger()

        val inspector =
            testRapid {
                sendTestMessage(BARNEPENSJON_SOEKNAD)
            }

        assertEquals(0, inspector.size)

        coVerify(exactly = 1) {
            behandlingKlientMock.finnEllerOpprettSak("25478323363", SakType.BARNEPENSJON)
            pdfgenKlient.genererPdf(any(), any())
            dokarkivKlientMock.opprettJournalpost(any())
        }
    }

    @Test
    fun `OMSTILLINGSSTOENAD - Feil ved journalføring, skal ikke sende melding`() {
        val sak = Sak("13848599411", SakType.OMSTILLINGSSTOENAD, randomSakId(), Enheter.PORSGRUNN.enhetNr, null, null)

        coEvery { behandlingKlientMock.finnEllerOpprettSak(any(), any()) } returns sak
        coEvery { pdfgenKlient.genererPdf(any(), any()) } returns "".toByteArray()
        coEvery { dokarkivKlientMock.opprettJournalpost(any()) } throws ResponseException(mockk(), "feil")
        coEvery { behandlingKlientMock.hentSakMedBehandlinger(any()) } returns opprettSakMedBehandlinger()

        val inspector =
            testRapid {
                sendTestMessage(OMSTILLINGSSTOENAD_SOEKNAD)
            }

        assertEquals(0, inspector.size)

        coVerify(exactly = 1) {
            behandlingKlientMock.finnEllerOpprettSak("13848599411", SakType.OMSTILLINGSSTOENAD)
            pdfgenKlient.genererPdf(any(), any())
            dokarkivKlientMock.opprettJournalpost(any())
        }
    }

    @Test
    fun `BARNEPENSJON - Ingen sak funnet eller opprettet, skal ikke sende melding`() {
        coEvery { behandlingKlientMock.finnEllerOpprettSak(any(), any()) } throws ResponseException(mockk(), "error")

        val inspector =
            testRapid {
                sendTestMessage(BARNEPENSJON_SOEKNAD)
            }

        assertEquals(0, inspector.size)
        coVerify(exactly = 1) { behandlingKlientMock.finnEllerOpprettSak("25478323363", SakType.BARNEPENSJON) }
        coVerify {
            dokarkivKlientMock wasNot Called
            pdfgenKlient wasNot Called
        }
    }

    @Test
    fun `OMSTILLINGSSTOENAD - Ingen sak funnet eller opprettet, skal ikke sende melding`() {
        coEvery { behandlingKlientMock.finnEllerOpprettSak(any(), any()) } throws ResponseException(mockk(), "error")

        val inspector =
            testRapid {
                sendTestMessage(OMSTILLINGSSTOENAD_SOEKNAD)
            }

        assertEquals(0, inspector.size)
        coVerify(exactly = 1) { behandlingKlientMock.finnEllerOpprettSak("13848599411", SakType.OMSTILLINGSSTOENAD) }
        coVerify {
            dokarkivKlientMock wasNot Called
            pdfgenKlient wasNot Called
        }
    }

    @Test
    fun `soeker mangler eller har ugyldig fnr - skal journalfoere uten sak`() {
        coEvery { pdfgenKlient.genererPdf(any(), any()) } returns "".toByteArray()
        coEvery { dokarkivKlientMock.opprettJournalpost(any(), any()) } returns OpprettJournalpostResponse("123", false)

        val inspector =
            testRapid {
                sendTestMessage(UGYLDIG_FNR_SOEKNAD)
            }

        assertEquals(1, inspector.size)

        val melding = inspector.message(0)
        assertEquals(
            SoeknadInnsendtHendelseType.EVENT_NAME_INNSENDT.lagEventnameForType(),
            melding.get(EVENT_NAME_KEY).asText(),
        )
        assertNull(melding.get(GyldigSoeknadVurdert.sakIdKey))
        assertFalse(melding.get(FordelerFordelt.soeknadFordeltKey).asBoolean())

        coVerify {
            dokarkivKlientMock.opprettJournalpost(any(), false)
            pdfgenKlient.genererPdf(any(), any())
            behandlingKlientMock wasNot Called
        }
    }

    private fun opprettSakMedBehandlinger(behandlinger: List<BehandlingSammendrag> = emptyList()) =
        SakMedBehandlinger(mockk(), behandlinger)

    private fun testRapid(block: TestRapid.() -> Unit) =
        TestRapid()
            .apply {
                NySoeknadRiver(this, behandlingKlientMock, journalfoerSoeknadService)
                block()
            }.inspektør

    companion object {
        val BARNEPENSJON_SOEKNAD = readFile("/innsendtsoeknad/barnepensjon.json")
        val OMSTILLINGSSTOENAD_SOEKNAD = readFile("/innsendtsoeknad/omstillingsstoenad.json")
        val UGYLDIG_FNR_SOEKNAD = readFile("/innsendtsoeknad/ugyldig_fnr.json")

        private fun readFile(file: String) = NySoeknadRiverTest::class.java.getResource(file)!!.readText()
    }
}
