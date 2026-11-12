package no.nav.etterlatte.brukerdialog.soeknad.journalfoering

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.brukerdialog.soeknad.pdf.PdfGeneratorKlient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.funksjonsbrytere.PdfgenrsFeatureToggle
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.SoeknadInnsendt
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.InnsendtSoeknad
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.Sak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.treeToValue

internal class JournalfoerSoeknadServiceTest {
    private val dokarkivKlient = mockk<DokarkivKlient>()

    @Test
    fun `journalfoerer ikke naar pdfgenrs svarer med feil`() {
        val pdfgenKlient =
            PdfGeneratorKlient(
                HttpClient(
                    MockEngine {
                        assertEquals(
                            "http://ey-pdfgenrs/api/v1/genpdf/eypdfgen/utvidet_omstillingsstoenad_v1",
                            it.url.toString(),
                        )
                        respond("Unknown path '/api/v1/genpdf/eypdfgen/utvidet_omstillingsstoenad_v1'", HttpStatusCode.NotFound)
                    },
                ),
                "http://ey-pdfgen/api/v1/genpdf/eypdfgen",
                "http://ey-pdfgenrs/api/v1/genpdf/eypdfgen",
                { PdfgenrsFeatureToggle.brukPdfgenrs(it, mockk()) },
            )
        val service = JournalfoerSoeknadService(dokarkivKlient, pdfgenKlient)

        val hendelse = objectMapper.readTree(javaClass.getResource("/innsendtsoeknad/utvidet_omstillingsstoenad.json")!!.readText())
        val soeknad = objectMapper.treeToValue<InnsendtSoeknad>(hendelse[SoeknadInnsendt.skjemaInfoKey])
        val sak = Sak("13848599411", SakType.OMSTILLINGSSTOENAD, randomSakId(), Enheter.PORSGRUNN.enhetNr, null, null)

        assertNull(service.opprettJournalpost(43, sak, soeknad))
        coVerify(exactly = 0) { dokarkivKlient.opprettJournalpost(any(), any()) }
    }
}
