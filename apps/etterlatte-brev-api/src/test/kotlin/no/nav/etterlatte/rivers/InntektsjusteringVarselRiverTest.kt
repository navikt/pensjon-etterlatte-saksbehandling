package no.nav.etterlatte.rivers

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class InntektsjusteringVarselRiverTest {
    private fun testRapid() = TestRapid().apply { InntektsjusteringVarselRiver(this) }

    @Test
    fun `skal starte inntektsjustering varsel jobb`() {
        // TODO: test
    }
}
