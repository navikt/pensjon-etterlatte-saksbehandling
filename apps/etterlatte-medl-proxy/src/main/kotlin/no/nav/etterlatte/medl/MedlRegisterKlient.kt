package no.nav.etterlatte.medl

import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import org.slf4j.LoggerFactory

class MedlRegisterKlient(
    private val host: String,
    private val client: HttpClient
) {

    private val logger = LoggerFactory.getLogger(MedlRegisterKlient::class.java)

    suspend fun hentMedlemskapsunntak(fnr: String, token: String): ObjectNode? {
        try {
            val response = client.get("$host/api/v1/medlemskapsunntak") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header("Nav-Call-Id", "etterlatte-medl-proxy")
                header("Nav-Consumer-Id", "etterlatte-medl-proxy")
                header("Nav-Personident", fnr)
            }

            return response.body()
        } catch (e: Exception) {
            logger.error("Feil ved kall til MEDL: ", e)
            throw e
        }
    }

    suspend fun hentInnsyn(fnr: String, token: String): ObjectNode? {
        try {
            val response = client.get("$host/api/v1/innsyn/person") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header("Nav-Call-Id", "etterlatte-medl-proxy")
                header("Nav-Consumer-Id", "etterlatte-medl-proxy")
                header("Nav-Personident", fnr)
            }

            return response.body()
        } catch (e: Exception) {
            logger.error("Feil ved kall til MEDL: ", e)
            throw e
        }
    }
}