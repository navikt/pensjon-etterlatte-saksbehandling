package no.nav.etterlatte.migrering.verifisering

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.HentPersongalleriRequest
import no.nav.etterlatte.libs.common.person.PersonRolle

internal class PdlTjenesterKlient(config: Config, private val pdl_app: HttpClient) {
    private val url = config.getString("pdltjenester.url")

    fun hentPerson(
        rolle: PersonRolle,
        folkeregisteridentifikator: Folkeregisteridentifikator,
    ): PersonDTO =
        runBlocking {
            val response =
                pdl_app.post("$url/person/v2") {
                    contentType(ContentType.Application.Json)
                    setBody(HentPersonRequest(folkeregisteridentifikator, rolle, SakType.BARNEPENSJON))
                }
            if (response.status.isSuccess()) {
                response.body<PersonDTO>()
            } else {
                throw IllegalStateException("Fant ikke informasjon i PDL for person med rolle $rolle")
            }
        }

    fun hentPersongalleri(soeker: Folkeregisteridentifikator): Persongalleri =
        runBlocking {
            val response =
                pdl_app.post("$url/galleri") {
                    contentType(ContentType.Application.Json)
                    setBody(HentPersongalleriRequest(soeker, null, SakType.BARNEPENSJON))
                }
            if (response.status.isSuccess()) {
                response.body<Persongalleri>()
            } else {
                throw InternfeilException("Kunne ikke hente persongalleri for $soeker fra PDL")
            }
        }
}
