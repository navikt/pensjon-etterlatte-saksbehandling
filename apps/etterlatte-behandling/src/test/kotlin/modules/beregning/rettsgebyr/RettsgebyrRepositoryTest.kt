package no.nav.etterlatte.modules.beregning.rettsgebyr

import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

internal class RettsgebyrRepositoryTest {
    @Test
    fun `skal hente historiske rettsgebyr`() {
        RettsgebyrRepository.historiskeRettsgebyr shouldHaveAtLeastSize 2
    }

    @ParameterizedTest
    @CsvSource(
        "2024-01-01, 1277",
        "2025-01-01, 1314",
    )
    fun `skal lese gyldighetsdato og beloep fra JSON`(
        gyldigFra: LocalDate,
        forventetBeloep: Int,
    ) {
        val rettsgebyr = RettsgebyrRepository.historiskeRettsgebyr.single { it.gyldigFra == gyldigFra }

        rettsgebyr.rettsgebyr.toInteger() shouldBe forventetBeloep
    }
}
