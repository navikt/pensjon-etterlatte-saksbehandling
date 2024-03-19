package no.nav.etterlatte.libs.common.behandling

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class SakTypeTest {
    @Test
    fun `Temaer er korrekt`() {
        SakType.BARNEPENSJON.tema shouldBe "EYB"
        SakType.OMSTILLINGSSTOENAD.tema shouldBe "EYO"
    }

    @Test
    fun `Behandlingsnummer er korrekt`() {
        SakType.BARNEPENSJON.behandlingsnummer shouldBe "B359"
        SakType.OMSTILLINGSSTOENAD.behandlingsnummer shouldBe "B373"
    }
}
