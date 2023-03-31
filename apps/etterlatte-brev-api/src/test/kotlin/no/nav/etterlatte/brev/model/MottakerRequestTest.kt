package no.nav.etterlatte.brev.model

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.brev.adresse.RegoppslagResponseDTO
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

            val mottaker = MottakerRequest.fraRegoppslag(regoppslagResponseDTO)

            mottaker.adresse shouldBe "Testveien 13A"
            mottaker.navn shouldBe regoppslagResponseDTO.navn
            mottaker.land shouldBe regoppslagResponseDTO.adresse.land
            mottaker.postnummer shouldBe regoppslagResponseDTO.adresse.postnummer
            mottaker.poststed shouldBe regoppslagResponseDTO.adresse.poststed
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

            val mottaker = MottakerRequest.fraRegoppslag(regoppslagResponseDTO)

            mottaker.adresse shouldBe "c/o STOR SNERK, Testveien 13A"
            mottaker.navn shouldBe regoppslagResponseDTO.navn
            mottaker.land shouldBe regoppslagResponseDTO.adresse.land
            mottaker.postnummer shouldBe regoppslagResponseDTO.adresse.postnummer
            mottaker.poststed shouldBe regoppslagResponseDTO.adresse.poststed
        }
    }
}