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
import no.nav.etterlatte.brev.adresse.Norg2Klient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

internal class Norg2KlientTest {
    private val porsgrunnEnhetJson = javaClass.getResource("/norg2/porsgrunn_enhet.json")!!.readText()
    private val porsgrunnKontaktinfoJson = javaClass.getResource("/norg2/porsgrunn_kontaktinfo.json")!!.readText()
    private val errorJson = "{\"field\": null, \"message\": \"Enheten med nummeret '1234' eksisterer ikke\"}"
    private val defaultHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

    private val klient = mockHttpClient()

    companion object {
        private const val PORSGRUNN = "0805"
        private const val UKJENT = "1234"
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
        assertNull(kontaktinfo.epost)
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
                            "/enhet/$PORSGRUNN" -> respond(porsgrunnEnhetJson, HttpStatusCode.OK, defaultHeaders)
                            "/enhet/$PORSGRUNN/kontaktinformasjon" ->
                                respond(
                                    porsgrunnKontaktinfoJson,
                                    HttpStatusCode.OK,
                                    defaultHeaders,
                                )
                            "/enhet/$UKJENT" -> respond(errorJson, HttpStatusCode.NotFound, defaultHeaders)
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
