package no.nav.etterlatte.brev.norg2

import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.adresse.Norg2Epost
import no.nav.etterlatte.brev.adresse.Norg2Klient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.Enhetsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

internal class Norg2KlientTest {
    private val porsgrunnEnhetJson = javaClass.getResource("/norg2/porsgrunn_enhet.json")!!.readText()
    private val porsgrunnKontaktinfoJson = javaClass.getResource("/norg2/porsgrunn_kontaktinfo.json")!!.readText()
    private val errorJson = "{\"field\": null, \"message\": \"Enheten med nummeret '1234' eksisterer ikke\"}"
    private val defaultHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

    val klient = mockHttpClient()

    companion object {
        private val PORSGRUNN = Enheter.PORSGRUNN.enhetNr
        private val UKJENT = Enhetsnummer.ukjent
    }

    @Test
    fun `Uthenting av enhet OK`() {
        val enhet =
            runBlocking {
                klient.hentEnhet(PORSGRUNN)
            }

        assertEquals(PORSGRUNN, enhet.enhetNr)
        assertEquals("NAV Porsgrunn", enhet.navn)
        assertEquals("LOKAL", enhet.type)

        val kontaktinfo = enhet.kontaktinfo!!
        assertEquals(Norg2Epost(adresse = "porsgrunn@nav.no", kommentar = "ingen", kunIntern = false), kontaktinfo.epost)
        assertEquals("55553333", kontaktinfo.telefonnummer)
        assertEquals("stedsadresse", kontaktinfo.postadresse!!.type)
        assertEquals("3915", kontaktinfo.postadresse!!.postnummer)
        assertEquals("PORSGRUNN", kontaktinfo.postadresse!!.poststed)
    }

    @Test
    fun `Enhet ikke funnet`() {
        runBlocking {
            try {
                klient.hentEnhet(UKJENT)
                fail()
            } catch (e: Exception) {
                e.cause?.message shouldContain "Enheten med nummeret '1234' eksisterer ikke"
            }
        }
    }

    private fun mockHttpClient(): Norg2Klient {
        val httpClient =
            HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        when (request.url.fullPath) {
                            "/enhet/${PORSGRUNN.enhetNr}" -> respond(porsgrunnEnhetJson, HttpStatusCode.OK, defaultHeaders)
                            "/enhet/${PORSGRUNN.enhetNr}/kontaktinformasjon" ->
                                respond(
                                    porsgrunnKontaktinfoJson,
                                    HttpStatusCode.OK,
                                    defaultHeaders,
                                )
                            "/enhet/${UKJENT.enhetNr}" -> respond(errorJson, HttpStatusCode.NotFound, defaultHeaders)
                            else -> error("Unhandled ${request.url.fullPath}")
                        }
                    }
                }
                expectSuccess = true
                install(ContentNegotiation) { jackson { } }
            }

        return Norg2Klient("", httpClient)
    }
}
