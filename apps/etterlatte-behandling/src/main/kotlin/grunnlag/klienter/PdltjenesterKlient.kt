package no.nav.etterlatte.grunnlag.klienter

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.HentPersonHistorikkForeldreAnsvarRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.HentPersongalleriRequest
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.pdl.HistorikkForeldreansvar
import no.nav.etterlatte.sikkerLogg
import org.slf4j.LoggerFactory

interface PdlTjenesterKlient {
    fun hentPerson(
        foedselsnummer: String,
        rolle: PersonRolle,
        sakType: SakType,
    ): Person

    fun hentOpplysningsperson(
        foedselsnummer: String,
        rolle: PersonRolle,
        sakType: SakType,
    ): PersonDTO

    fun hentHistoriskForeldreansvar(
        fnr: Folkeregisteridentifikator,
        rolle: PersonRolle,
        sakType: SakType,
    ): HistorikkForeldreansvar

    suspend fun hentPersongalleri(
        foedselsnummer: String,
        sakType: SakType,
        innsender: String?,
    ): Persongalleri?
}

class PdlTjenesterKlientImpl(
    private val pdl: HttpClient,
    private val url: String,
) : PdlTjenesterKlient {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun hentPerson(
        foedselsnummer: String,
        rolle: PersonRolle,
        sakType: SakType,
    ): Person {
        val personRequest = HentPersonRequest(Folkeregisteridentifikator.of(foedselsnummer), rolle, listOf(sakType))
        val response =
            runBlocking {
                pdl
                    .post("$url/person") {
                        contentType(ContentType.Application.Json)
                        setBody(personRequest)
                    }.body<Person>()
            }
        return response
    }

    override suspend fun hentPersongalleri(
        foedselsnummer: String,
        sakType: SakType,
        innsender: String?,
    ): Persongalleri? =
        try {
            val persongalleriRequest =
                HentPersongalleriRequest(
                    mottakerAvYtelsen = Folkeregisteridentifikator.of(foedselsnummer),
                    saktype = sakType,
                    innsender =
                        innsender
                            ?.takeIf { Folkeregisteridentifikator.isValid(it) }
                            ?.let { Folkeregisteridentifikator.of(it) },
                )
            pdl
                .post("$url/galleri") {
                    contentType(ContentType.Application.Json)
                    setBody(persongalleriRequest)
                }.body<Persongalleri>()
        } catch (e: Exception) {
            logger.warn(
                "Kunne ikke hente persongalleriet fra PDL, på grunn av feil. " +
                    "Metadata om request er i sikkerlogg",
                e,
            )
            sikkerLogg.warn(
                "Kunne ikke hente persongalleriet fra PDL for soeker=$foedselsnummer i saktype " +
                    "$sakType, på grunn av feil.",
                e,
            )
            null
        }

    override fun hentOpplysningsperson(
        foedselsnummer: String,
        rolle: PersonRolle,
        sakType: SakType,
    ): PersonDTO {
        val personRequest = HentPersonRequest(Folkeregisteridentifikator.of(foedselsnummer), rolle, listOf(sakType))
        val response =
            runBlocking {
                pdl
                    .post("$url/person/v2") {
                        contentType(ContentType.Application.Json)
                        setBody(personRequest)
                    }.body<PersonDTO>()
            }
        return response
    }

    override fun hentHistoriskForeldreansvar(
        fnr: Folkeregisteridentifikator,
        rolle: PersonRolle,
        sakType: SakType,
    ): HistorikkForeldreansvar {
        val personRequest = HentPersonHistorikkForeldreAnsvarRequest(fnr, rolle, sakType)
        return runBlocking {
            pdl
                .post("$url/foreldreansvar") {
                    contentType(ContentType.Application.Json)
                    setBody(personRequest)
                }.body<HistorikkForeldreansvar>()
        }
    }
}
