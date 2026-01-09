package institusjonsopphold.personer

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.institusjonsopphold.model.InstitusjonsoppholdForPersoner

interface InstitusjonsoppholdInternKlient {
    suspend fun hentInstitusjonsopphold(personIdenter: List<String>): InstitusjonsoppholdForPersoner
}

/**
 * Se swagger for ekstern data-modell:
 * https://inst2-q2.dev.intern.nav.no/swagger-ui/index.html#/institusjonsopphold/institusjonsoppholdBulk
 */
class InstitusjonsoppholdInternKlientImpl(
    val resourceUrl: String,
    val institusjonsoppholdHttpClient: HttpClient,
) : InstitusjonsoppholdInternKlient {
    override suspend fun hentInstitusjonsopphold(personIdenter: List<String>): InstitusjonsoppholdForPersoner =
        institusjonsoppholdHttpClient
            .post("$resourceUrl/api/personer/institusjonsopphold") {
                contentType(ContentType.Application.Json)
                setBody(HentOppholdRequest(personIdenter))
            }.body()
}

class InstitusjonsoppholdInternKlientTest : InstitusjonsoppholdInternKlient {
    override suspend fun hentInstitusjonsopphold(personIdenter: List<String>): InstitusjonsoppholdForPersoner =
        InstitusjonsoppholdForPersoner(emptyMap())
}

data class HentOppholdRequest(
    val personIdenter: List<String>,
)
