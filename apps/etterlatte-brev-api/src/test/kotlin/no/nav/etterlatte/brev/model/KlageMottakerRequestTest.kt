package no.nav.etterlatte.brev.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import no.nav.etterlatte.brev.adresse.RegoppslagResponseDTO
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

internal class KlageMottakerRequestTest {
    @Nested
    inner class MappingFraRegoppslag {
        @Test
        fun `Person med vanlig adresse`() {
            val regoppslagResponseDTO =
                RegoppslagResponseDTO(
                    "Test Testesen",
                    adresse =
                        RegoppslagResponseDTO.Adresse(
                            type = RegoppslagResponseDTO.AdresseType.NORSKPOSTADRESSE,
                            adresseKilde = RegoppslagResponseDTO.AdresseKilde.BOSTEDSADRESSE,
                            adresselinje1 = "Testveien 13A",
                            adresselinje2 = null,
                            adresselinje3 = null,
                            postnummer = "0123",
                            poststed = "Teststed",
                            landkode = "NO",
                            land = "NORGE",
                        ),
                )

            val mottaker = mottakerFraAdresse(SOEKER_FOEDSELSNUMMER, regoppslagResponseDTO, MottakerType.HOVED)

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
            val regoppslagResponseDTO =
                RegoppslagResponseDTO(
                    "Test Testesen",
                    adresse =
                        RegoppslagResponseDTO.Adresse(
                            type = RegoppslagResponseDTO.AdresseType.NORSKPOSTADRESSE,
                            adresseKilde = RegoppslagResponseDTO.AdresseKilde.BOSTEDSADRESSE,
                            adresselinje1 = "c/o STOR SNERK",
                            adresselinje2 = "Testveien 13A",
                            adresselinje3 = null,
                            postnummer = "0123",
                            poststed = "Teststed",
                            landkode = "NO",
                            land = "NORGE",
                        ),
                )

            val mottaker = mottakerFraAdresse(SOEKER_FOEDSELSNUMMER, regoppslagResponseDTO, MottakerType.HOVED)

            mottaker.navn shouldBe regoppslagResponseDTO.navn
            mottaker.adresse.adresselinje1 shouldBe "c/o STOR SNERK"
            mottaker.adresse.adresselinje2 shouldBe "Testveien 13A"
            mottaker.adresse.adresselinje3 shouldBe null
            mottaker.adresse.postnummer shouldBe regoppslagResponseDTO.adresse.postnummer
            mottaker.adresse.poststed shouldBe regoppslagResponseDTO.adresse.poststed
            mottaker.adresse.land shouldBe regoppslagResponseDTO.adresse.land
            mottaker.adresse.landkode shouldBe regoppslagResponseDTO.adresse.landkode
        }

        @Test
        fun `Sjekk gyldighet av mottaker fungerer - felles uavhengig av land`() {
            val manglerNavn = Mottaker(UUID.randomUUID(), "", adresse = mockk())

            manglerNavn.erGyldig() shouldNotBe emptyList<String>()

            val manglerFnrOgOrgnr =
                Mottaker(
                    UUID.randomUUID(),
                    "Navn Navnesen",
                    foedselsnummer = null,
                    orgnummer = null,
                    adresse = mockk(),
                )

            manglerFnrOgOrgnr.erGyldig() shouldNotBe emptyList<String>()

            val manglerLand =
                Mottaker(
                    UUID.randomUUID(),
                    "Navn",
                    foedselsnummer = MottakerFoedselsnummer(SOEKER_FOEDSELSNUMMER.value),
                    adresse = Adresse("type", postnummer = "1234", poststed = "", landkode = "NO", land = ""),
                )

            manglerLand.erGyldig() shouldNotBe emptyList<String>()

            val manglerLandkode =
                Mottaker(
                    UUID.randomUUID(),
                    "Navn",
                    foedselsnummer = MottakerFoedselsnummer(SOEKER_FOEDSELSNUMMER.value),
                    adresse = Adresse("type", postnummer = "1234", poststed = "", landkode = "", land = "Norge"),
                )

            manglerLandkode.erGyldig() shouldNotBe emptyList<String>()
        }

        @Test
        fun `Sjekk gyldighet av mottaker fungerer - NORSKPOSTADRESSE`() {
            val manglerPoststed =
                Mottaker(
                    UUID.randomUUID(),
                    "Navn",
                    foedselsnummer = MottakerFoedselsnummer(SOEKER_FOEDSELSNUMMER.value),
                    adresse = Adresse("NORSKPOSTADRESSE", postnummer = "1234", poststed = "", landkode = "NO", land = "Norge"),
                )

            manglerPoststed.erGyldig() shouldNotBe emptyList<String>()

            val manglerPostnummer =
                Mottaker(
                    UUID.randomUUID(),
                    "Navn",
                    foedselsnummer = MottakerFoedselsnummer(SOEKER_FOEDSELSNUMMER.value),
                    adresse = Adresse("NORSKPOSTADRESSE", postnummer = "", poststed = "Oslo", landkode = "NO", land = "Norge"),
                )

            manglerPostnummer.erGyldig() shouldNotBe emptyList<String>()
        }

        @Test
        fun `Sjekk gyldighet av mottaker fungerer - UTENLANDSKPOSTADRESSE`() {
            val manglerAdresselinje =
                Mottaker(
                    UUID.randomUUID(),
                    "Navn",
                    foedselsnummer = MottakerFoedselsnummer(SOEKER_FOEDSELSNUMMER.value),
                    adresse =
                        Adresse(
                            "UTENLANDSKPOSTADRESSE",
                            adresselinje1 = "",
                            postnummer = "",
                            poststed = "",
                            landkode = "NO",
                            land = "Norge",
                        ),
                )

            manglerAdresselinje.erGyldig() shouldNotBe emptyList<String>()
        }
    }
}
