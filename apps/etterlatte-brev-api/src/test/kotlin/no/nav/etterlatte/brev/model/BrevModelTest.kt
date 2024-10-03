package no.nav.etterlatte.brev.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import org.junit.jupiter.api.Test

internal class BrevModelTest {
    @Test
    fun `Validering av mottaker og adresse`() {
        opprettMottaker(navn = "").erGyldig() shouldNotBe emptyList<String>()

        opprettMottaker(foedselsnummer = null).erGyldig() shouldNotBe emptyList<String>()
        opprettMottaker(foedselsnummer = MottakerFoedselsnummer("")).erGyldig() shouldNotBe emptyList<String>()

        opprettMottaker(orgnummer = null).erGyldig() shouldNotBe emptyList<String>()
        opprettMottaker(orgnummer = "").erGyldig() shouldNotBe emptyList<String>()

        opprettMottaker(adresseType = "").erGyldig() shouldNotBe emptyList<String>()

        opprettMottaker(adresseType = "NORSKPOSTADRESSE", poststed = null).erGyldig() shouldNotBe emptyList<String>()
        opprettMottaker(adresseType = "NORSKPOSTADRESSE", poststed = "").erGyldig() shouldNotBe emptyList<String>()

        opprettMottaker(adresseType = "NORSKPOSTADRESSE", postnummer = null).erGyldig() shouldNotBe emptyList<String>()
        opprettMottaker(adresseType = "NORSKPOSTADRESSE", postnummer = "").erGyldig() shouldNotBe emptyList<String>()

        opprettMottaker(
            adresseType = "UTENLANDSKPOSTADRESSE",
            adresselinje1 = null,
        ).erGyldig() shouldNotBe emptyList<String>()

        opprettMottaker(landkode = "").erGyldig() shouldNotBe emptyList<String>()
        opprettMottaker(land = "").erGyldig() shouldNotBe emptyList<String>()

        opprettMottaker(
            adresseType = "UTENLANDSKPOSTADRESSE",
            adresselinje1 = "adresselinje1",
            postnummer = null,
            poststed = null,
        ).erGyldig() shouldBe emptyList()
    }

    private fun opprettMottaker(
        navn: String = "Test Testesen",
        foedselsnummer: MottakerFoedselsnummer? = MottakerFoedselsnummer(SOEKER_FOEDSELSNUMMER.value),
        orgnummer: String? = "999888777",
        adresseType: String = "NORSKPOSTADRESSE",
        adresselinje1: String? = null,
        adresselinje2: String? = null,
        adresselinje3: String? = null,
        postnummer: String? = null,
        poststed: String? = null,
        landkode: String = "NO",
        land: String = "Norge",
    ) = Mottaker(
        navn,
        foedselsnummer,
        orgnummer,
        adresse =
            Adresse(
                adresseType,
                adresselinje1,
                adresselinje2,
                adresselinje3,
                postnummer,
                poststed,
                landkode,
                land,
            ),
    )
}
