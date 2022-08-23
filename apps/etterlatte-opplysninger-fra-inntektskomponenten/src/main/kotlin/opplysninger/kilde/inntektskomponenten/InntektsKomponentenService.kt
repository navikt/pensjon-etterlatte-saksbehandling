package no.nav.etterlatte.opplysninger.kilde.inntektskomponenten

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.InntektsKomponenten
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth

class InntektsKomponentenService(
    private val inntektskomponentenClient: HttpClient,
    private val url: String
) : InntektsKomponenten {
    private val logger = LoggerFactory.getLogger(InntektsKomponentenService::class.java)

    override fun hentInntektListe(fnr: Foedselsnummer, doedsdato: LocalDate): InntektsKomponentenResponse {
        logger.info("Henter inntektliste fra inntektskomponenten")

        val hentInntektlisteRequest = HentInntektListeRequestBody(
            ident = InntektListeIdent(fnr.value, "NATURLIG_IDENT"),
            ainntektsfilter = "Etterlatteytelser",
            maanedFom = YearMonth.from(doedsdato.minusYears(5)).toString(),
            maanedTom = YearMonth.from(doedsdato).toString(),
            formaal = "Etterlatteytelser"
        )

        // Her må det muligens gjøres ett kall pr år. PGA tregheter mot eksterne systemer. Vi får bare teste
        return runBlocking {
            val response = inntektskomponentenClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(hentInntektlisteRequest)
            }

            logger.info("hentInntektListe: " + response.bodyAsText())

            response.body()
        }
    }
}