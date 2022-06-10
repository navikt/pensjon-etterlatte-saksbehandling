package no.nav.etterlatte.opplysninger.kilde.inntektskomponenten

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.InntektsKomponenten
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class InntektsKomponentenService(private val inntektskomponentenClient: HttpClient, private val url: String) :
    InntektsKomponenten {
    private val logger = LoggerFactory.getLogger(InntektsKomponentenService::class.java)

    override fun hentInntektListe(fnr: Foedselsnummer, doedsdato: LocalDate): InntektsKomponentenResponse {
        logger.info("Henter inntektliste fra inntektskomponenten")

        YearMonth.from(doedsdato)

        val hentInntektlisteRequest =
            HentInntektListeRequestBody(
                InntektListeIdent(fnr.value, "NATURLIG_IDENT"),
                "Etterlatteytelser",
                YearMonth.from(doedsdato.minusYears(5)).toString(),
                YearMonth.from(doedsdato).toString(),
                "Etterlatteytelser"
            )


        // Her må det muligens gjøres ett kall pr år. PGA tregheter mot eksterne systemer. Vi får bare teste
        val inntektsListe = runBlocking {
                inntektskomponentenClient.post<InntektsKomponentenResponse>(url) {
                    contentType(ContentType.Application.Json)
                    body = hentInntektlisteRequest
                }
        }

        print(inntektsListe)

        return inntektsListe;

    }


}