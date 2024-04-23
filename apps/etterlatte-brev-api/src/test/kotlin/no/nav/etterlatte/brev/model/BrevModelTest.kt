package no.nav.etterlatte.brev.model

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import org.junit.jupiter.api.Test

internal class BrevModelTest {
    @Test
    fun `Validering av mottaker og adresse`() {
        opprettMottaker(navn = "").erGyldig() shouldBe false

        opprettMottaker(foedselsnummer = null).erGyldig() shouldBe false
        opprettMottaker(foedselsnummer = Folkeregisteridentifikator.ofNullable("")).erGyldig() shouldBe false

        opprettMottaker(orgnummer = null).erGyldig() shouldBe false
        opprettMottaker(orgnummer = "").erGyldig() shouldBe false

        opprettMottaker(adresseType = "").erGyldig() shouldBe false

        opprettMottaker(adresseType = "NORSKPOSTADRESSE", poststed = null).erGyldig() shouldBe false
        opprettMottaker(adresseType = "NORSKPOSTADRESSE", poststed = "").erGyldig() shouldBe false

        opprettMottaker(adresseType = "NORSKPOSTADRESSE", postnummer = null).erGyldig() shouldBe false
        opprettMottaker(adresseType = "NORSKPOSTADRESSE", postnummer = "").erGyldig() shouldBe false

        opprettMottaker(
            adresseType = "UTENLANDSKPOSTADRESSE",
            adresselinje1 = null,
        ).erGyldig() shouldBe false

        opprettMottaker(landkode = "").erGyldig() shouldBe false
        opprettMottaker(land = "").erGyldig() shouldBe false

        opprettMottaker(
            adresseType = "UTENLANDSKPOSTADRESSE",
            adresselinje1 = "adresselinje1",
            postnummer = null,
            poststed = null,
        ).erGyldig() shouldBe true
    }

    private fun opprettMottaker(
        navn: String = "Test Testesen",
        foedselsnummer: Folkeregisteridentifikator? = SOEKER_FOEDSELSNUMMER,
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
