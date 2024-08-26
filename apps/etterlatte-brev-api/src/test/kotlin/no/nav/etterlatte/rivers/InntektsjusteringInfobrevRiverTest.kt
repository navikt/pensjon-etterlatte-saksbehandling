package no.nav.etterlatte.rivers

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class InntektsjusteringInfobrevRiverTest {
    private fun testRapid() = TestRapid().apply { InntektsjusteringInfobrevRiver(this) }

    @Test
    fun `skal starte inntektsjustering infobrev jobb`() {
        // TODO: test
    }
}
