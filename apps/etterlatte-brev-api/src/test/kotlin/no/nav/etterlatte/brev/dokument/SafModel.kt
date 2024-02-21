package no.nav.etterlatte.brev.dokument

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import no.nav.etterlatte.libs.common.deserialize
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class SafModel {
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
}
