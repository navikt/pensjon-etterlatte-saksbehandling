package no.nav.etterlatte.brev.model

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.brev.adresse.RegoppslagResponseDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class MottakerRequestTest {

    @Nested
    inner class MappingFraRegoppslag {
        @Test
        fun `Person med vanlig adresse`() {
            val regoppslagResponseDTO = RegoppslagResponseDTO(
                "Test Testesen",
                adresse = RegoppslagResponseDTO.Adresse(
                    type = RegoppslagResponseDTO.AdresseType.NORSKPOSTADRESSE,
                    adresseKilde = RegoppslagResponseDTO.AdresseKilde.BOSTEDSADRESSE,
                    adresselinje1 = "Testveien 13A",
                    adresselinje2 = null,
                    adresselinje3 = null,
                    postnummer = "0123",
                    poststed = "Teststed",
                    landkode = "NO",
                    land = "NORGE"
                )
            )

            val mottaker = Mottaker.fra(STOR_SNERK, regoppslagResponseDTO)

            mottaker.navn shouldBe regoppslagResponseDTO.navn
            mottaker.adresse.adresselinje1 shouldBe "Testveien 13A"
            mottaker.adresse.adresselinje2 shouldBe null
            mottaker.adresse.adresselinje3 shouldBe null
            mottaker.adresse.postnummer shouldBe regoppslagResponseDTO.adresse.postnummer
            mottaker.adresse.poststed shouldBe regoppslagResponseDTO.adresse.poststed
            mottaker.adresse.land shouldBe regoppslagResponseDTO.adresse.land
            mottaker.adresse.landkode shouldBe regoppslagResponseDTO.adresse.landkode
        }

        @Test
        fun `Person har co adresse`() {
            val regoppslagResponseDTO = RegoppslagResponseDTO(
                "Test Testesen",
                adresse = RegoppslagResponseDTO.Adresse(
                    type = RegoppslagResponseDTO.AdresseType.NORSKPOSTADRESSE,
                    adresseKilde = RegoppslagResponseDTO.AdresseKilde.BOSTEDSADRESSE,
                    adresselinje1 = "c/o STOR SNERK",
                    adresselinje2 = "Testveien 13A",
                    adresselinje3 = null,
                    postnummer = "0123",
                    poststed = "Teststed",
                    landkode = "NO",
                    land = "NORGE"
                )
            )

            val mottaker = Mottaker.fra(STOR_SNERK, regoppslagResponseDTO)

            mottaker.navn shouldBe regoppslagResponseDTO.navn
            mottaker.adresse.adresselinje1 shouldBe "c/o STOR SNERK"
            mottaker.adresse.adresselinje2 shouldBe "Testveien 13A"
            mottaker.adresse.adresselinje3 shouldBe null
            mottaker.adresse.postnummer shouldBe regoppslagResponseDTO.adresse.postnummer
            mottaker.adresse.poststed shouldBe regoppslagResponseDTO.adresse.poststed
            mottaker.adresse.land shouldBe regoppslagResponseDTO.adresse.land
            mottaker.adresse.landkode shouldBe regoppslagResponseDTO.adresse.landkode
        }
    }

    companion object {
        private val STOR_SNERK = Folkeregisteridentifikator.of("11057523044")
    }
}