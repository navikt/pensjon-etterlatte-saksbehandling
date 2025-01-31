package no.nav.etterlatte.brukerdialog.omsendring

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.brukerdialog.soeknad.client.BehandlingClient
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.AvsenderMottaker
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.Bruker
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.DokarkivKlient
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.JournalpostSak
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.OpprettJournalpostRequest
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.OpprettJournalpostResponse
import no.nav.etterlatte.brukerdialog.soeknad.pdf.PdfGeneratorKlient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.PDFMal
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OmsMeldtInnEndringRiverTest {
    private val behandlingKlientMock = mockk<BehandlingClient>()
    private val dokarkivKlientMock = mockk<DokarkivKlient>()
    private val pdfgenKlient = mockk<PdfGeneratorKlient>()

    private val journalfoerService = JournalfoerOmsMeldtInnEndringService(dokarkivKlientMock, pdfgenKlient)

    private fun testRapid() = TestRapid().apply { OmsMeldtInnEndringRiver(this, behandlingKlientMock, journalfoerService) }

    @Test
    fun `Skal journalføre meldt inn endring for OMS og lage oppgave`() {
        val sak = Sak("123", SakType.OMSTILLINGSSTOENAD, SakId(321), Enheter.PORSGRUNN.enhetNr)
        val endring =
            OmsMeldtInnEndring(
                id = UUID.randomUUID(),
                fnr = "123",
                endring = OmsEndring.ANNET,
                beskrivelse = "Endringer fra bruker..",
                tidspunkt = Instant.parse("2024-08-01T05:06:07Z"),
            )

        coEvery { behandlingKlientMock.finnEllerOpprettSak(any(), any()) } returns sak
        coEvery { pdfgenKlient.genererPdf(any(), any()) } returns "".toByteArray()
        coEvery { dokarkivKlientMock.opprettJournalpost(any()) } returns
            OpprettJournalpostResponse(
                "JournalId123",
                true,
            )
        coEvery { behandlingKlientMock.behandleInntektsjustering(any()) } just Runs

        val melding =
            JsonMessage
                .newMessage(
                    mapOf(
                        "@event_name" to OmsMeldtInnEndringHendelseKeys.HENDELSE_KEY,
                        OmsMeldtInnEndringHendelseKeys.INNHOLD_KEY to endring,
                    ),
                ).toJson()

        testRapid().apply { sendTestMessage(melding) }.inspektør

        val journalRequest = slot<OpprettJournalpostRequest>()
        val pdfDataSlot = slot<PDFMal>()

        coVerify(exactly = 1) {
            behandlingKlientMock.finnEllerOpprettSak("123", SakType.OMSTILLINGSSTOENAD)

            dokarkivKlientMock.opprettJournalpost(capture(journalRequest))
            pdfgenKlient.genererPdf(capture(pdfDataSlot), "oms_meldt_inn_endring_v1")

            behandlingKlientMock.opprettOppgave(
                sakId = SakId(321L),
                withArg {
                    it.oppgaveKilde shouldBe OppgaveKilde.HENDELSE
                    it.oppgaveType shouldBe OppgaveType.GENERELL_OPPGAVE
                    it.merknad shouldBe "" // TODO
                },
            )
        }
        with(journalRequest.captured) {
            tittel shouldBe "Meldt inn endring Omstillingstønad"
            tema shouldBe sak.sakType.tema
            journalfoerendeEnhet shouldBe sak.enhet
            avsenderMottaker shouldBe AvsenderMottaker(sak.ident)
            bruker shouldBe Bruker(sak.ident)
            eksternReferanseId shouldBe "etterlatte:omstillingsstoenad:omsMeldtInnEndring:${endring.id}"
            this.sak shouldBe JournalpostSak(sak.id.toString())
            dokumenter.size shouldBe 1
            dokumenter[0].tittel shouldBe "Meldt inn endring Omstillingstønad"
            dokumenter[0].dokumentvarianter.size shouldBe 1
            dokumenter[0].dokumentvarianter[0].filtype shouldBe "PDFA"
            dokumenter[0].dokumentvarianter[0].variantformat shouldBe "ARKIV"
        }

        val pdfData = objectMapper.readValue<ArkiverOmsMeldtInnEndring>(pdfDataSlot.captured.toJson())
        with(pdfData) {
            sakId.sakId shouldBe 321L
            type shouldBe "ANNET"
            endringer shouldBe "Endringer fra bruker.."
            tidspunkt shouldBe "01.08.2024 05:06:07"
        }
    }
}
