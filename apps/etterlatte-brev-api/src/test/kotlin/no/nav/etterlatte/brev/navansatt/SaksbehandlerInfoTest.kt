package no.nav.etterlatte.brev.navansatt

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class SaksbehandlerInfoTest {
    @Test
    fun `Skal returnere navnet paa formatet Fornavn Etternavn`() {
        SaksbehandlerInfo("Z111111", "Etternavn, Fornavn", "Fornavn", "Etternavn", "fornavn.etternavn@nav.no").let {
            it.fornavnEtternavn shouldBe "Fornavn Etternavn"
        }
    }
}
