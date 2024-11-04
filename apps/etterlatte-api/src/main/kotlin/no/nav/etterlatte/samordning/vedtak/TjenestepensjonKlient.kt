package no.nav.etterlatte.samordning.vedtak

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.objectMapper
import org.slf4j.LoggerFactory
import java.time.LocalDate

class TjenestepensjonKlient(
    config: Config,
    private val httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(TjenestepensjonKlient::class.java)
    private val sikkerLogg = sikkerlogger()

    private val tjenestepensjonUrl = "${config.getString("tjenestepensjon.url")}/api/tjenestepensjon"

    suspend fun harTpYtelseOnDate(
        fnr: String,
        tpnr: Tjenestepensjonnummer,
        fomDato: LocalDate,
    ): Boolean {
        logger.info("Sjekk om det finnes tjenestepensjonsytelse pr $fomDato for ordning '$tpnr'")

        val tpNumre: TpNumre =
            handleFeil(fnr, tpnr, fomDato) {
                httpClient
                    .get {
                        url("$tjenestepensjonUrl/tpNrWithYtelse?fomDate=$fomDato")
                        header("fnr", fnr)
                    }.let { deserialize<TpNumre>(it.body()) }
            }

        return tpNumre.tpNr.contains(tpnr.value)
    }

    private suspend inline fun <T> handleFeil(
        fnr: String,
        tpnr: Tjenestepensjonnummer,
        fomDato: LocalDate,
        block: () -> T,
    ): T {
        try {
            return block.invoke()
        } catch (e: ClientRequestException) {
            when (e.response.status) {
                HttpStatusCode.Unauthorized -> {
                    logger.error("Feil ved tilgang til TP-registeret", e)
                    throw TjenestepensjonInternFeil("TP: Ikke tilgang")
                }

                HttpStatusCode.BadRequest -> {
                    logger.error("Feil ved input til TP-registeret", e)
                    throw TjenestepensjonUgyldigForesporselException("TP: Ugyldig forespørsel")
                }

                HttpStatusCode.NotFound -> {
                    val tpError = e.toTjenestepensjonFeil()
                    if (tpError.message.contains("Person ikke funnet")) {
                        sikkerLogg.warn("Sjekk av TP feilet for [fnr=$fnr, fomDato=$fomDato, tpNr=$tpnr]")
                        throw TjenestepensjonIkkeFunnetException("TP: Person ikke funnet")
                    } else {
                        logger.error("Fant ikke forespurt ressurs i TP-registeret", e)
                        throw TjenestepensjonInternFeil("TP: Ressurs ikke funnet")
                    }
                }

                else -> {
                    logger.error("Feil i kontroll mot TP-registeret", e)
                    throw e
                }
            }
        }
    }
}

internal data class TpNumre(
    @field:JsonSetter(nulls = Nulls.AS_EMPTY)
    val tpNr: List<String> = emptyList(),
)

data class TjenestepensjonFeil(
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val timestamp: Tidspunkt,
)

private suspend fun ClientRequestException.toTjenestepensjonFeil() =
    objectMapper.readValue(this.response.bodyAsText(), TjenestepensjonFeil::class.java)
