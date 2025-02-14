package no.nav.etterlatte.brev.dokument

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import no.nav.etterlatte.brev.dokarkiv.AvsenderMottakerIdType
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.toJson
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import no.nav.etterlatte.brev.dokarkiv.AvsenderMottaker as DokarkivAvsenderMottaker
import no.nav.etterlatte.brev.dokument.AvsenderMottaker as SafAvsenderMottaker

class SafModelTest {
    @ParameterizedTest
    @CsvSource(
        value = [
            "forbidden",
            "not_found",
            "bad_request",
            "server_error",
        ],
    )
    fun `Saf error code deserialiseres korrekt`(code: String) {
        val json =
            """
            {
                "code": "$code",
                "classification": "ExecutionAborted"
            }
            """.trimIndent()

        val deserialized: Error.Extensions = deserialize(json)

        deserialized.code shouldNotBe null
        deserialized.code shouldBe Error.Code.of(code)
    }

    @Test
    fun `Error-respons fra Saf deserialiseres korrekt`() {
        val json =
            """
            {
              "errors": [
                {
                  "message": "Fant ikke journalpost i fagarkivet. journalpostId=999999999",
                  "locations": [
                    {
                      "line": 2,
                      "column": 3
                    }
                  ],
                  "path": [
                    "journalpost"
                  ],
                  "extensions": {
                    "code": "not_found",
                    "classification": "ExecutionAborted"
                  }
                }
              ],
              "data": {
                "journalpost": null
              }
            }
            """.trimIndent()

        val deserialized: JournalpostResponse = deserialize(json)

        deserialized.data!!.journalpost shouldBe null
        deserialized.errors!!.size shouldBe 1

        with(deserialized.errors!!.single()) {
            message shouldContain "Fant ikke journalpost"
            path.first() shouldBe "journalpost"
            extensions!!.code shouldBe Error.Code.NOT_FOUND
            extensions!!.classification shouldBe "ExecutionAborted"
        }
    }

    @Test
    fun `SERDE - Saf avsendermottaker til dokarkiv avsendermottaker`() {
        val safJson = """{"id":"id","type":"FNR","navn":"Navn Navnesen","land":"Norge","erLikBruker":false}"""

        val safAvsenderMottaker = deserialize<SafAvsenderMottaker>(safJson)
        safAvsenderMottaker.type shouldBe AvsenderMottakerIdType.FNR

        val safAvsenderMottakerJson = safAvsenderMottaker.toJson()

        val dokarkivAvsenderMottaker =
            deserialize<DokarkivAvsenderMottaker>(safAvsenderMottakerJson)

        safAvsenderMottaker.id shouldBe dokarkivAvsenderMottaker.id
        safAvsenderMottaker.type shouldBe dokarkivAvsenderMottaker.idType
        safAvsenderMottaker.navn shouldBe dokarkivAvsenderMottaker.navn
        safAvsenderMottaker.land shouldBe dokarkivAvsenderMottaker.land
    }

    @ParameterizedTest
    @EnumSource(AvsenderMottakerIdType::class)
    fun `Deserialisering av avsendermottaker idtype`(type: AvsenderMottakerIdType) {
        val safJson = """{"id":"id","type":"$type","navn":"Navn Navnesen","land":"Norge","erLikBruker":false}"""
        val safAvsenderMottaker = deserialize<SafAvsenderMottaker>(safJson)

        safAvsenderMottaker.type shouldBe type
    }

    @Test
    fun `Deserialisering av avsendermottaker idtype som er NULL`() {
        val safJson = """{"id":"id","type":"NULL","navn":"Navn Navnesen","land":"Norge","erLikBruker":false}"""
        val safAvsenderMottaker = deserialize<SafAvsenderMottaker>(safJson)

        safAvsenderMottaker.type shouldBe AvsenderMottakerIdType.NULL
    }
}
