package no.nav.etterlatte.brev.model

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInnvilgelse
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OmstillingsstoenadInnvilgelseTest {
    @Test
    fun `innvilgetMindreEnnFireMndEtterDoedsfall`() {
        OmstillingsstoenadInnvilgelse.innvilgetMindreEnnFireMndEtterDoedsfall(
            LocalDate.of(2024, 9, 1),
            LocalDate.of(2024, 8, 20),
        ) shouldBe true

        // negative
        OmstillingsstoenadInnvilgelse.innvilgetMindreEnnFireMndEtterDoedsfall(
            LocalDate.of(2024, 5, 2),
            LocalDate.of(2024, 1, 1),
        ) shouldBe false
    }
}
