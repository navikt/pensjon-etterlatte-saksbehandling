package no.nav.etterlatte.brukerdialog.inntektsjustering

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.behandling.randomSakId
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
import no.nav.etterlatte.libs.common.event.InntektsjusteringInnsendt
import no.nav.etterlatte.libs.common.event.InntektsjusteringInnsendtHendelseType
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.PDFMal
import no.nav.etterlatte.libs.common.inntektsjustering.Inntektsjustering
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class InntektsjusteringRiverTest {
    private val behandlingKlientMock = mockk<BehandlingClient>()
    private val dokarkivKlientMock = mockk<DokarkivKlient>()
    private val pdfgenKlient = mockk<PdfGeneratorKlient>()

    private val journalfoerInntektsjusteringService =
        JournalfoerInntektsjusteringService(dokarkivKlientMock, pdfgenKlient)

    private fun testRapid() = TestRapid().apply { InntektsjusteringRiver(this, behandlingKlientMock, journalfoerInntektsjusteringService) }

    @Test
    fun `Skal journalføre inntektsjustering og starte behandling med summert inntekt`() {
        val sak = Sak("123", SakType.OMSTILLINGSSTOENAD, randomSakId(), Enheter.PORSGRUNN.enhetNr, null, null)
        val inntektsjustering =
            Inntektsjustering(
                id = UUID.randomUUID(),
                fnr = "123",
                inntektsaar = 2025,
                arbeidsinntekt = 100,
                naeringsinntekt = 200,
                inntektFraUtland = 300,
                afpInntekt = 400,
                afpTjenesteordning = "tjenesteordning",
                skalGaaAvMedAlderspensjon = "JA",
                datoForAaGaaAvMedAlderspensjon = LocalDate.of(2025, 6, 10),
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
                        "@event_name" to InntektsjusteringInnsendtHendelseType.EVENT_NAME_INNSENDT.eventname,
                        InntektsjusteringInnsendt.inntektsjusteringInnhold to inntektsjustering,
                    ),
                ).toJson()

        testRapid().apply { sendTestMessage(melding) }.inspektør

        val journalRequest = slot<OpprettJournalpostRequest>()
        val pdfDataSlot = slot<PDFMal>()

        coVerify(exactly = 1) {
            behandlingKlientMock.finnEllerOpprettSak("123", SakType.OMSTILLINGSSTOENAD)

            dokarkivKlientMock.opprettJournalpost(capture(journalRequest))
            pdfgenKlient.genererPdf(capture(pdfDataSlot), "inntektsjustering_nytt_aar_v1")

            behandlingKlientMock.behandleInntektsjustering(
                withArg {
                    it.sak shouldBe sak.id
                    it.inntektsjusteringId shouldBe inntektsjustering.id
                    it.arbeidsinntekt shouldBe 100
                    it.naeringsinntekt shouldBe 200
                    it.afpInntekt shouldBe 400
                    it.inntektFraUtland shouldBe 300
                    it.mottattDato shouldBe LocalDateTime.ofInstant(inntektsjustering.tidspunkt, norskTidssone)
                    it.datoForAaGaaAvMedAlderspensjon shouldBe YearMonth.of(2025, 6)
                },
            )
        }
        with(journalRequest.captured) {
            tittel shouldBe "Inntektsjustering 2025"
            tema shouldBe sak.sakType.tema
            journalfoerendeEnhet shouldBe sak.enhet
            avsenderMottaker shouldBe AvsenderMottaker(sak.ident)
            bruker shouldBe Bruker(sak.ident)
            eksternReferanseId shouldBe "etterlatte:omstillingsstoenad:inntektsjustering:${inntektsjustering.id}"
            this.sak shouldBe JournalpostSak(sak.id.toString())
            dokumenter.size shouldBe 1
            dokumenter[0].tittel shouldBe "Inntektsjustering 2025"
            dokumenter[0].dokumentvarianter.size shouldBe 1
            dokumenter[0].dokumentvarianter[0].filtype shouldBe "PDFA"
            dokumenter[0].dokumentvarianter[0].variantformat shouldBe "ARKIV"
        }

        val pdfData = objectMapper.readValue<ArkiverInntektsjustering>(pdfDataSlot.captured.toJson())
        with(pdfData) {
            aar shouldBe 2025
            arbeidsinntekt shouldBe 100
            naeringsinntekt shouldBe 200
            inntektFraUtland shouldBe 300
            afpInntekt shouldBe 400
            afpTjenesteordning shouldBe "tjenesteordning"
            skalGaaAvMedAlderspensjon shouldBe "JA"
            datoForAaGaaAvMedAlderspensjon shouldBe "10.06.2025"
            tidspunkt shouldBe "01.08.2024 05:06:07"
        }
    }
}
