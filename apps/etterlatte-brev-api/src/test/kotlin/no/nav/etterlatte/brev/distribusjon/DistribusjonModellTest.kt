package no.nav.etterlatte.brev.distribusjon

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.toJson
import org.junit.jupiter.api.Test

internal class DistribusjonModellTest {
    @Test
    fun `verifiser serde`() {
        val distAdresse =
            Adresse(
                adressetype = AdresseType.NORSK,
                adresselinje1 = "adresselinje1",
                adresselinje2 = "adresselinje2",
                adresselinje3 = "adresselinje3",
                postnummer = "postnummer",
                poststed = "poststed",
                landkode = "NOR",
            )

        val serialized = distAdresse.toJson()

        serialized shouldNotContain "landkode"
        serialized shouldContain "norskPostadresse"

        val deserialized = deserialize<Adresse>(serialized)

        deserialized shouldBe distAdresse
    }
}
