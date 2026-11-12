package no.nav.etterlatte.brukerdialog.soeknad.pdf

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.funksjonsbrytere.PdfgenrsFeatureToggle
import no.nav.etterlatte.libs.common.event.SoeknadInnsendt
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.InnsendtSoeknad
import no.nav.etterlatte.libs.common.objectMapper
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tools.jackson.module.kotlin.treeToValue

internal class PdfGeneratorKlientTest {
    @ParameterizedTest
    @CsvSource(
        "utvidet_omstillingsstoenad_v1, false, ey-pdfgenrs",
        "omstillingsstoenad_v1, false, ey-pdfgen",
        "omstillingsstoenad_v1, true, ey-pdfgenrs",
    )
    fun `bruker pdfgenrs for utvidet OMS og beholder bryterstyring for eksisterende maler`(
        mal: String,
        bryterAktiv: Boolean,
        forventetHost: String,
    ) = runBlocking {
        val featureToggleService = mockk<FeatureToggleService>()
        every { featureToggleService.isEnabled(any(), false, any()) } returns bryterAktiv
        val pdf = "%PDF-1.7".toByteArray()
        val hendelse = objectMapper.readTree(javaClass.getResource("/innsendtsoeknad/utvidet_omstillingsstoenad.json")!!.readText())
        val soeknad = objectMapper.treeToValue<InnsendtSoeknad>(hendelse[SoeknadInnsendt.skjemaInfoKey])

        HttpClient(
            MockEngine {
                assertEquals("http://$forventetHost/api/v1/genpdf/eypdfgen/$mal", it.url.toString())
                respond(pdf)
            },
        ).use { httpClient ->
            val klient =
                PdfGeneratorKlient(
                    httpClient,
                    "http://ey-pdfgen/api/v1/genpdf/eypdfgen",
                    "http://ey-pdfgenrs/api/v1/genpdf/eypdfgen",
                    { PdfgenrsFeatureToggle.brukPdfgenrs(it, featureToggleService) },
                )

            assertArrayEquals(pdf, klient.genererPdf(soeknad, mal))
        }
    }
}
