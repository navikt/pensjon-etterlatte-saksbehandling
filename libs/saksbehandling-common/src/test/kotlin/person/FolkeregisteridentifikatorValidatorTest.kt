package no.nav.etterlatte.person

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.person.FolkeregisteridentifikatorValidator.Companion.isValid
import org.junit.jupiter.api.Test

internal class FolkeregisteridentifikatorValidatorTest {
    @Test
    fun `Sjekk diverse gyldige test fnr`() {
        isValid("02438311109") shouldBe true
        isValid("18498248795") shouldBe true
        isValid("09438336165") shouldBe true
        isValid("31488338237") shouldBe true
        isValid("16508201382") shouldBe true
        isValid("27458328671") shouldBe true
    }

    @Test
    fun `Sjekk diverse ugyldige numeriske verdier`() {
        isValid("1234") shouldBe false

        isValid("00000000000") shouldBe false
        isValid("11111111111") shouldBe false
        isValid("22222222222") shouldBe false
        isValid("33333333333") shouldBe false
        isValid("44444444444") shouldBe false
        isValid("55555555555") shouldBe false
        isValid("66666666666") shouldBe false
        isValid("77777777777") shouldBe false
        isValid("88888888888") shouldBe false
        isValid("99999999999") shouldBe false

        isValid("36117512737") shouldBe false
        isValid("12345678901") shouldBe false
        isValid("00000000001") shouldBe false
        isValid("10000000000") shouldBe false
    }

    @Test
    fun `Sjekk diverse ugyldige tekst verdier`() {
        isValid("") shouldBe false
        isValid("hei") shouldBe false
        isValid("gyldigfnrmedtekst11057523044") shouldBe false
    }
}
