package no.nav.etterlatte.utbetaling.simulering

import io.kotest.matchers.collections.shouldHaveSize
import no.nav.etterlatte.utbetaling.readFile
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningResponse
import org.junit.jupiter.api.Test

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
        resultat.etterbetaling shouldHaveSize 1
        resultat.tilbakekreving shouldHaveSize 1
    }
}
