package no.nav.etterlatte.pdl

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.KorrektIPDL
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import org.slf4j.LoggerFactory

interface Pdl {
    fun hentPdlModell(foedselsnummer: String, rolle: PersonRolle): Person
}

class PdlService(
    private val pdl_app: HttpClient,
    private val url: String
) : Pdl {

    companion object {
        val logger = LoggerFactory.getLogger(PdlService::class.java)
    }

    override fun hentPdlModell(foedselsnummer: String, rolle: PersonRolle): Person {
        logger.info("Henter Pdl-modell for ${rolle.name}")
        val personRequest = HentPersonRequest(Foedselsnummer.of(foedselsnummer), rolle)
        val response = runBlocking {
            pdl_app.post("$url/person") {
                contentType(ContentType.Application.Json)
                setBody(personRequest)
            }.body<Person>()
        }
        return response
    }

    fun personErDoed(fnr: String): KorrektIPDL {
        return hentPdlModell(
            foedselsnummer = fnr,
            rolle = PersonRolle.BARN
        ).doedsdato?.let { doedsdato ->
            logger.info(
                "Person med fnr $fnr er doed i pdl " +
                    "med doedsdato: $doedsdato"
            )
            KorrektIPDL.JA
        } ?: KorrektIPDL.NEI
    }

    fun personHarUtflytting(fnr: String): KorrektIPDL {
        return if (!hentPdlModell(
                foedselsnummer = fnr,
                rolle = PersonRolle.BARN
            ).utland?.utflyttingFraNorge.isNullOrEmpty()
        ) {
            KorrektIPDL.JA
        } else {
            KorrektIPDL.NEI
        }
    }

    /*
    TODO: sjekk om forelder-barn-relasjon er gyldig
     */
    fun forelderBarnRelasjonErGyldig(fnr: String): KorrektIPDL {
        return KorrektIPDL.IKKE_SJEKKET
    }
}