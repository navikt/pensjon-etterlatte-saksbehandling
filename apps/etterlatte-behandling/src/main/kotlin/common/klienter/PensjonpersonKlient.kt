package no.nav.etterlatte.common.klienter

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.person.maskerFnr
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

interface PensjonpersonKlient {
    suspend fun harSammeBostedsadresse(harSammeBostedsAdresseRequest: HarSammeBostedsAdresseRequest): BorSammen
}

class PensjonpersonKlientmpl(config: Config, private val httpClient: HttpClient) : PensjonpersonKlient {
    private val baseUrlSuffix = "/api/adresse"
    private val url = "${config.getString("pensjonperson.url")}$baseUrlSuffix"

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PensjonpersonKlientmpl::class.java)
    }

    override suspend fun harSammeBostedsadresse(harSammeBostedsAdresseRequest: HarSammeBostedsAdresseRequest): BorSammen {
        logger.info(
            "Sjekker om samme bostedsadresse for ${harSammeBostedsAdresseRequest.person.maskerFnr()}" +
                " og ${harSammeBostedsAdresseRequest.annenPerson.maskerFnr()}",
        )
        return httpClient.get("$url/bostedsadresse/borSammen") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header("pid", harSammeBostedsAdresseRequest.person)
            header("annenPid", harSammeBostedsAdresseRequest.annenPerson)
            header("dato", harSammeBostedsAdresseRequest.forDato)
        }.body<BorSammen>()
    }
}

data class HarSammeBostedsAdresseRequest(
    val person: String,
    val annenPerson: String,
    val forDato: LocalDate,
)

data class BorSammen(val status: Boolean)
