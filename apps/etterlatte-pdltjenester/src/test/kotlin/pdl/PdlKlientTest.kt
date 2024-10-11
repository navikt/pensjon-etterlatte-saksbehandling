package no.nav.etterlatte.pdl

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.JacksonConverter
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.STOR_SNERK
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.HentAdressebeskyttelseRequest
import no.nav.etterlatte.libs.common.person.HentFolkeregisterIdenterForAktoerIdBolkRequest
import no.nav.etterlatte.libs.common.person.HentGeografiskTilknytningRequest
import no.nav.etterlatte.libs.common.person.HentPdlIdentRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.PersonIdent
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PdlKlientTest {
    private lateinit var pdlKlient: PdlKlient

    @Test
    fun `hentPerson returnerer gyldig person`() {
        mockEndpoint("/pdl/person.json")

        runBlocking {
            val personResponse =
                pdlKlient.hentPerson(
                    HentPersonRequest(STOR_SNERK, PersonRolle.BARN, listOf(SakType.BARNEPENSJON)),
                )
            val hentPerson = personResponse.data?.hentPerson

            assertEquals("LITEN", hentPerson?.navn?.first()?.fornavn)
            assertEquals("HEST", hentPerson?.navn?.first()?.etternavn)
            assertEquals(LocalDate.of(2007, 8, 29), hentPerson?.foedselsdato?.first()?.foedselsdato)
            assertEquals(2007, hentPerson?.foedselsdato?.first()?.foedselsaar)
            assertEquals("NOR", hentPerson?.foedested?.first()?.foedeland)
        }
    }

    @Test
    fun `hentPersonBolk returnerer gyldige personer`() {
        mockEndpoint("/pdl/personBolk.json")

        runBlocking {
            val personResponse = pdlKlient.hentPersonBolk(listOf(STOR_SNERK), listOf(SakType.BARNEPENSJON))
            val hentPerson = personResponse.data?.hentPersonBolk

            assertEquals(
                "TRIVIELL",
                hentPerson
                    ?.first()
                    ?.person
                    ?.navn
                    ?.first()
                    ?.fornavn,
            )
            assertEquals(
                "SKILPADDE",
                hentPerson
                    ?.first()
                    ?.person
                    ?.navn
                    ?.first()
                    ?.etternavn,
            )
            assertEquals(
                "1987-07-30",
                hentPerson
                    ?.first()
                    ?.person
                    ?.foedselsdato
                    ?.first()
                    ?.foedselsdato
                    ?.toString(),
            )
            assertEquals(
                1987,
                hentPerson
                    ?.first()
                    ?.person
                    ?.foedselsdato
                    ?.first()
                    ?.foedselsaar,
            )
            assertEquals(
                "FJI",
                hentPerson
                    ?.first()
                    ?.person
                    ?.foedested
                    ?.first()
                    ?.foedeland,
            )

            assertEquals(
                "ROBUST",
                hentPerson
                    ?.get(1)
                    ?.person
                    ?.navn
                    ?.first()
                    ?.fornavn,
            )
        }
    }

    @Test
    fun `hentPerson returnerer ikke funnet`() {
        mockEndpoint("/pdl/person_ikke_funnet.json")

        runBlocking {
            val personResponse =
                pdlKlient.hentPerson(
                    HentPersonRequest(STOR_SNERK, PersonRolle.BARN, listOf(SakType.BARNEPENSJON)),
                )
            val errors = personResponse.errors

            assertEquals("Fant ikke person", errors?.first()?.message)
        }
    }

    @Test
    fun `hentFolkeregisterIdent returnerer folkeregisterident`() {
        mockEndpoint("/pdl/folkeregisterident.json")

        runBlocking {
            val identResponse =
                pdlKlient.hentPdlIdentifikator(
                    HentPdlIdentRequest(PersonIdent(AVDOED_FOEDSELSNUMMER.value)),
                )
            assertEquals(
                "09508229892",
                identResponse.data
                    ?.hentIdenter
                    ?.identer
                    ?.first()
                    ?.ident,
            )
            assertEquals(
                false,
                identResponse.data
                    ?.hentIdenter
                    ?.identer
                    ?.first()
                    ?.historisk,
            )
            assertEquals(
                "FOLKEREGISTERIDENT",
                identResponse.data
                    ?.hentIdenter
                    ?.identer
                    ?.first()
                    ?.gruppe,
            )
        }
    }

    @Test
    fun `hentFolkeregisterIdent returnerer ikke funnet`() {
        mockEndpoint("/pdl/ident_ikke_funnet.json")

        runBlocking {
            val personResponse =
                pdlKlient.hentPdlIdentifikator(
                    HentPdlIdentRequest(PersonIdent("1234")),
                )
            val errors = personResponse.errors

            assertEquals("Fant ikke person", errors?.first()?.message)
        }
    }

    @Test
    fun `hentFolkregisterIdentBolk returnerer bolk`() {
        mockEndpoint("/pdl/folkeregisteridentBolk.json")

        runBlocking {
            val identResponse =
                pdlKlient.hentFolkeregisterIdenterForAktoerIdBolk(
                    HentFolkeregisterIdenterForAktoerIdBolkRequest(setOf("2082995739063")),
                )
            assertEquals("2082995739063", identResponse.first().ident)
            assertEquals(
                "03486048831",
                identResponse
                    .first()
                    .identer!!
                    .first()
                    .ident,
            )
            assertEquals(
                false,
                identResponse
                    .first()
                    .identer!!
                    .first()
                    .historisk,
            )
            assertEquals(
                "FOLKEREGISTERIDENT",
                identResponse
                    .first()
                    .identer!!
                    .first()
                    .gruppe.name,
            )
        }
    }

    @Test
    fun `hentGeografiskTilknytning returnerer geografisk tilknytning`() {
        mockEndpoint("/pdl/geografisk_tilknytning.json")

        runBlocking {
            val personResponse =
                pdlKlient.hentGeografiskTilknytning(
                    HentGeografiskTilknytningRequest(STOR_SNERK, SakType.BARNEPENSJON),
                )

            assertEquals("0301", personResponse.data?.hentGeografiskTilknytning?.gtKommune)
            assertEquals(PdlGtType.KOMMUNE, personResponse.data?.hentGeografiskTilknytning?.gtType)
        }
    }

    @Test
    fun `hentAdressebeskyttelse returnerer adressebeskyttelse`() {
        mockEndpoint("/pdl/adressebeskyttelse.json")

        runBlocking {
            val response =
                pdlKlient.hentAdressebeskyttelse(
                    HentAdressebeskyttelseRequest(PersonIdent(STOR_SNERK.value), SakType.BARNEPENSJON),
                )

            assertEquals(
                PdlGradering.FORTROLIG,
                response.data
                    ?.hentPerson
                    ?.adressebeskyttelse
                    ?.single()
                    ?.gradering,
            )
        }
    }

    private fun mockEndpoint(jsonUrl: String) {
        val httpClient =
            HttpClient(MockEngine) {
                expectSuccess = true
                engine {
                    addHandler { request ->
                        when (request.url.fullPath) {
                            "" -> {
                                val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                                val json = javaClass.getResource(jsonUrl)!!.readText()
                                respond(json, headers = headers)
                            }
                            else -> error(request.url.fullPath)
                        }
                    }
                }
                install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            }

        pdlKlient = PdlKlient(httpClient, "")
    }
}
