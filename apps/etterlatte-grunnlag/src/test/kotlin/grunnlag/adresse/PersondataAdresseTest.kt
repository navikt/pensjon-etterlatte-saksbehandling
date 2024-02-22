package no.nav.etterlatte.grunnlag.adresse

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.objectMapper
import org.junit.jupiter.api.Test

class PersondataAdresseTest {
    @Test
    fun `skal tolke json som en VergePersonFormat`() {
        val readValue: PersondataAdresse = objectMapper.readValue(EXAMPLE_VERGE_PERSON.trimIndent())
        readValue as VergePersonFormat

        readValue.type shouldBe "VERGE_PERSON_POSTADRESSE"
        readValue.adresseString shouldBe "c/o UTROLIG MAKTPERSON, Steinløysvegen 45, 6453 KLEIVE"
        readValue.adresselinjer shouldContainExactly listOf("c/o UTROLIG MAKTPERSON", "Steinløysvegen 45", "6453 KLEIVE")
        readValue.vergePid shouldBe "23816296951"
        readValue.navn shouldBe "UTROLIG MAKTPERSON"
        readValue.adresse.adresselinje1 shouldBe "Steinløysvegen 45"
        readValue.adresse.adresselinje2 shouldBe null
        readValue.adresse.adresselinje3 shouldBe null
        readValue.adresse.postnummer shouldBe "6453"
        readValue.adresse.poststed shouldBe "KLEIVE"
        readValue.adresse.landkode shouldBe "NO"
        readValue.adresse.land shouldBe "NORGE"
        readValue.adresse.type shouldBe AdresseType.NORSKPOSTADRESSE
        readValue.adresse.adresseKilde shouldBe AdresseKilde.BOSTEDSADRESSE

        val readAgain: PersondataAdresse = objectMapper.readValue(objectMapper.writeValueAsString(readValue))
        readAgain shouldBeEqual readValue
    }

    @Test
    fun `skal tolke json som en VergeSamhandlerFormat`() {
        val readValue: PersondataAdresse = objectMapper.readValue(EXAMPLE_VERGE_SAMHANDLER_ADRESSE)

        readValue as VergeSamhandlerFormat
        readValue.type shouldBe "VERGE_SAMHANDLER_POSTADRESSE"
        readValue.adresselinjer shouldContainExactly listOf("c/o ADVOKAT BERG  JOHN", "Noe her også", "Postboks 260", "7402 TRONDHEIM")
        readValue.adresseString shouldBe "c/o ADVOKAT BERG  JOHN, Postboks 260, 7402 TRONDHEIM"
        readValue.navn shouldBe "ADVOKAT BERG  JOHN"
        readValue.linje1 shouldBe "Postboks 260"
        readValue.linje2 shouldBe "L i n j e 2"
        readValue.linje3 shouldBe null
        readValue.postnr shouldBe "7402"
        readValue.poststed shouldBe "TRONDHEIM"
        readValue.land shouldBe "NORGE"
        readValue.landkode shouldBe "NOR"

        val readAgain: PersondataAdresse = objectMapper.readValue(objectMapper.writeValueAsString(readValue))
        readAgain shouldBeEqual readValue
    }

    @Test
    fun `skal tolke json som en RegoppslagAdresse`() {
        val readValue: PersondataAdresse = objectMapper.readValue(EXAMPLE_REGOPPSLAG_ADRESSE)

        readValue as RegoppslagFormat
        readValue.type shouldBe "REGOPPSLAG_ADRESSE"
        readValue.adresselinjer shouldContainExactly listOf("Dueholveien 205", "1735 VARTEIG")
        readValue.adresseString shouldBe "Dueholveien 205, 1735 VARTEIG"
        readValue.navn shouldBe "DISTINGVERT KARAKTERISTIKK"
        readValue.adresse.adresselinje1 shouldBe "Dueholveien 205"
        readValue.adresse.adresselinje2 shouldBe null
        readValue.adresse.adresselinje3 shouldBe null
        readValue.adresse.postnummer shouldBe "1735"
        readValue.adresse.poststed shouldBe "VARTEIG"
        readValue.adresse.landkode shouldBe "NO"
        readValue.adresse.land shouldBe "NORGE"
        readValue.adresse.type shouldBe AdresseType.NORSKPOSTADRESSE
        readValue.adresse.adresseKilde shouldBe AdresseKilde.BOSTEDSADRESSE

        val readAgain: PersondataAdresse = objectMapper.readValue(objectMapper.writeValueAsString(readValue))
        readAgain shouldBeEqual readValue
    }

    companion object {
        private val EXAMPLE_REGOPPSLAG_ADRESSE =
            """
            {
              "navn": "DISTINGVERT KARAKTERISTIKK",
              "adresse": {
                "adresselinje1": "Dueholveien 205",
                "adresselinje2": null,
                "adresselinje3": null,
                "postnummer": "1735",
                "poststed": "VARTEIG",
                "landkode": "NO",
                "land": "NORGE",
                "type": "NORSKPOSTADRESSE",
                "adresseKilde": "BOSTEDSADRESSE"
              },
              "type": "REGOPPSLAG_ADRESSE",
              "adresseString": "Dueholveien 205, 1735 VARTEIG",
              "adresselinjer": [
                "Dueholveien 205",
                "1735 VARTEIG"
              ]
            }
            """.trimIndent()
        private val EXAMPLE_VERGE_PERSON = """
            {
              "vergePid": "23816296951",
              "navn": "UTROLIG MAKTPERSON",
              "adresse": {
                "adresselinje1": "Steinløysvegen 45",
                "adresselinje2": null,
                "adresselinje3": null,
                "postnummer": "6453",
                "poststed": "KLEIVE",
                "landkode": "NO",
                "land": "NORGE",
                "type": "NORSKPOSTADRESSE",
                "adresseKilde": "BOSTEDSADRESSE"
              },
              "type": "VERGE_PERSON_POSTADRESSE",
              "adresseString": "c/o UTROLIG MAKTPERSON, Steinløysvegen 45, 6453 KLEIVE",
              "adresselinjer": [
                "c/o UTROLIG MAKTPERSON",
                "Steinløysvegen 45",
                "6453 KLEIVE"
              ]
            }
            """
        private val EXAMPLE_VERGE_SAMHANDLER_ADRESSE =
            """
            {
              "navn": "ADVOKAT BERG  JOHN",
              "linje1": "Postboks 260",
              "linje2": "L i n j e 2",
              "linje3": null,
              "postnr": "7402",
              "poststed": "TRONDHEIM",
              "land": "NORGE",
              "landkode": "NOR",
              "type": "VERGE_SAMHANDLER_POSTADRESSE",
              "adresseString": "c/o ADVOKAT BERG  JOHN, Postboks 260, 7402 TRONDHEIM",
              "adresselinjer": [
                "c/o ADVOKAT BERG  JOHN",
                "Noe her også",
                "Postboks 260",
                "7402 TRONDHEIM"
              ]
            }
            """.trimIndent()
    }
}
