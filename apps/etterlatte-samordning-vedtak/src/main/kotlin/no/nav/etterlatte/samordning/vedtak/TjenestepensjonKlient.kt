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
import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import org.slf4j.LoggerFactory
import java.time.LocalDate

class TjenestepensjonKlient(config: Config, private val httpClient: HttpClient) {
    private val logger = LoggerFactory.getLogger(TjenestepensjonKlient::class.java)
    private val sikkerLogg = sikkerlogger()

    private val tjenestepensjonUrl = "${config.getString("tjenestepensjon.url")}/api/tjenestepensjon"

    suspend fun harTpForholdByDate(
        fnr: String,
        tpnr: String,
        fomDato: LocalDate,
    ): Boolean {
        logger.info("Sjekk om det finnes tjenestepensjonsforhold pr $fomDato for ordning '$tpnr'")

        val tp: SamhandlerPersonDto =
            try {
                httpClient.get {
                    url("$tjenestepensjonUrl/finnForholdForBruker?datoFom=$fomDato")
                    header("fnr", fnr)
                    header("tpnr", tpnr)
                }.body()
            } catch (e: ClientRequestException) {
                sikkerLogg.error("Feil ved sjekk av tjenestepensjonsforhold for [fnr=$fnr, fomDato=$fomDato, tpNr=$tpnr]", e)
                when (e.response.status) {
                    HttpStatusCode.Unauthorized -> throw TjenestepensjonManglendeTilgangException("TP: Ikke tilgang", e)
                    HttpStatusCode.BadRequest -> throw TjenestepensjonUgyldigForesporselException("TP: Ugyldig forespørsel", e)
                    HttpStatusCode.NotFound -> throw TjenestepensjonIkkeFunnetException("TP: Ressurs ikke funnet", e)
                    else -> throw e
                }
            }

        return tp.forhold.isNotEmpty()
    }

    suspend fun harTpYtelseOnDate(
        fnr: String,
        tpnr: String,
        fomDato: LocalDate,
    ): Boolean {
        logger.info("Sjekk om det finnes tjenestepensjonsytelse pr $fomDato for ordning '$tpnr'")

        val tpNumre: TpNumre =
            try {
                httpClient.get {
                    url("$tjenestepensjonUrl/tpNrWithYtelse?fomDate=$fomDato")
                    header("fnr", fnr)
                }.let { deserialize<TpNumre>(it.body()) }
            } catch (e: ClientRequestException) {
                sikkerLogg.error("Feil ved sjekk av tjenestepensjonsytelse for [fnr=$fnr, fomDato=$fomDato, tpNr=$tpnr]", e)
                when (e.response.status) {
                    HttpStatusCode.Unauthorized -> throw TjenestepensjonManglendeTilgangException("TP: Ikke tilgang", e)
                    HttpStatusCode.BadRequest -> throw TjenestepensjonUgyldigForesporselException("TP: Ugyldig forespørsel", e)
                    HttpStatusCode.NotFound -> throw TjenestepensjonIkkeFunnetException("TP: Ressurs ikke funnet", e)
                    else -> throw e
                }
            }

        return tpNumre.tpNr.contains(tpnr)
    }
}

internal data class TpNumre(
    @field:JsonSetter(nulls = Nulls.AS_EMPTY)
    val tpNr: List<String> = emptyList(),
)
