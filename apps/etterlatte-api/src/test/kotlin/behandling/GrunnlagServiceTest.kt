package behandling

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.etterlatte.behandling.BehandlingKlient
import no.nav.etterlatte.behandling.GrunnlagKlient
import no.nav.etterlatte.behandling.GrunnlagService
import no.nav.etterlatte.kafka.TestProdusent
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.Customization
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.comparator.CustomComparator

internal class GrunnlagServiceTest {
    private val saksbehandlerId = "saksbehandlerId"
    private val token = "token"

    @Test
    fun `sender ut opplysning om soesken i beregning på riktig format`() = runTest {
        val mockedBehandling = mockk<DetaljertBehandling>()
        coEvery { mockedBehandling.sak }.returns(100L)
        val mockClient = mockk<BehandlingKlient>()
        coEvery { mockClient.hentBehandling(any(), any()) }.returns(mockedBehandling)
        val mockGrunnlagClient = mockk<GrunnlagKlient>()

        val testprodusent = TestProdusent<String, String>()
        val grunnlagService = GrunnlagService(mockClient, testprodusent, mockGrunnlagClient)

        val behandlingId = "behandlingId"
        val soeskenMedIBeregning = listOf(
            SoeskenMedIBeregning(Foedselsnummer.of("18057404783"), true),
            SoeskenMedIBeregning(Foedselsnummer.of("26017921265"), false)
        )

        grunnlagService.lagreSoeskenMedIBeregning(behandlingId, soeskenMedIBeregning, saksbehandlerId, token)
        val expected = """
            [
                {
                    "id":"eda32030-78ac-4c83-a9b3-750ad0198da4",
                    "kilde":{
                        "ident":"saksbehandlerId",
                        "tidspunkt":"2022-08-08T11:51:34.346022Z",
                        "type":"saksbehandler"
                    },
                    "opplysningType":"SAKSBEHANDLER_SOESKEN_I_BEREGNINGEN",
                    "meta":{},
                    "opplysning": {"beregningsgrunnlag" : [
                        {
                            "foedselsnummer":"18057404783",
                            "skalBrukes":true
                        },
                        {
                            "foedselsnummer":"26017921265",
                            "skalBrukes":false
                        }
                    ]},
                    "attestering":null
                }
            ]
        """.trimIndent()

        val actual = JSONObject(testprodusent.publiserteMeldinger.first().verdi).getString("opplysning")
        assertRapidMessage(actual, expected)
    }
}

private fun assertRapidMessage(expected: String, actual: String) {
    JSONAssert.assertEquals(
        expected,
        actual,
        CustomComparator(
            JSONCompareMode.LENIENT,
            Customization("*.id") { _: Any?, _: Any? -> true }, // Ignorer id-feltet
            Customization("*.kilde.tidspunkt") { _: Any?, _: Any? -> true }
        ) // Ignorer tidspunkt
    )
}