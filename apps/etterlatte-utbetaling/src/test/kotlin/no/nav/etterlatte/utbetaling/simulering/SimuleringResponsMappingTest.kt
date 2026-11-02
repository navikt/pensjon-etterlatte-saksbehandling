package no.nav.etterlatte.utbetaling.simulering

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.utbetaling.readFile
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningResponse
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SimuleringResponsMappingTest {
    private val objectMapper = simuleringObjectMapper()

    @Test
    fun `skal mappe simuleringsresultat av enkel OMS`() {
        val response = readFile("/simulering/oms_simple.json")

        val deserialized = objectMapper.readValue(response, SimulerBeregningResponse::class.java)
        val resultat = deserialized.simulering.tilSimulertBeregning(deserialized.infomelding.beskrMelding)

        resultat.kommendeUtbetalinger shouldHaveSize 2
        resultat.etterbetaling shouldHaveSize 0
        resultat.tilbakekreving shouldHaveSize 0
    }

    @Test
    fun `skal mappe simuleringsresultat av BT med tilbakekreving`() {
        val response = readFile("/simulering/bt_tilbakekreving.json")

        val deserialized = objectMapper.readValue(response, SimulerBeregningResponse::class.java)
        val resultat = deserialized.simulering.tilSimulertBeregning(deserialized.infomelding.beskrMelding)

        resultat.kommendeUtbetalinger shouldHaveSize 3
        resultat.kommendeUtbetalinger.filter { it.tilbakefoering } shouldHaveSize 1
        resultat.etterbetaling shouldHaveSize 5
        resultat.tilbakekreving shouldHaveSize 1
    }

    @Test
    fun `skal avgrense feilutbetaling til feilperioden naar samme aar har etterbetaling`() {
        val response = readFile("/simulering/bt_tilbakekreving.json")

        val deserialized = objectMapper.readValue(response, SimulerBeregningResponse::class.java)
        val resultat = deserialized.simulering.tilSimulertBeregning(deserialized.infomelding.beskrMelding)
        val oppsummeringer = requireNotNull(resultat.oppsummeringer)
        val oppsummeringForAar = oppsummeringer.perAar.single().oppsummering

        oppsummeringer.perAar.single().aarstall shouldBe 2024
        with(requireNotNull(oppsummeringForAar.feilutbetaling)) {
            brutto shouldBe BigDecimal("9885.00")
            beloepBrukerenSkulleHatt shouldBe BigDecimal("989.00")
            netto shouldBe BigDecimal("8896.00")
        }
        oppsummeringForAar.etterbetaling.brutto shouldBe BigDecimal("451.00")
        oppsummeringForAar.etterbetaling.netto shouldBe BigDecimal("451.00")
        oppsummeringer.forPerioden shouldBe oppsummeringForAar
    }
}
