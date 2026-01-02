package institusjonsopphold.personer

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.institusjonsopphold.model.InstitusjonsoppholdForPersoner

class InstitusjonsoppholdInternKlient(
    val config: Config,
    val institusjonsoppholdHttpClient: HttpClient,
) {
    private val resourceUrl = config.getString("institusjonsopphold.resource.url")

    suspend fun hentInstitusjonsopphold(personIdenter: List<String>): InstitusjonsoppholdForPersoner =
        institusjonsoppholdHttpClient
            .post("$resourceUrl/api/personer/institusjonsopphold") {
                contentType(ContentType.Application.Json)
                setBody(HentOppholdRequest(personIdenter))
            }.body()
}

data class HentOppholdRequest(
    val personIdenter: List<String>,
)
