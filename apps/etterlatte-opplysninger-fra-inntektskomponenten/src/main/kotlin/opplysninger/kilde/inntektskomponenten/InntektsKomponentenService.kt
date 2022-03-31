package no.nav.etterlatte.opplysninger.kilde.inntektskomponenten

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.InntektsKomponenten
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import org.slf4j.LoggerFactory

class InntektsKomponentenService(private val inntektskomponentenClient: HttpClient, private val url: String) :
    InntektsKomponenten {
    private val logger = LoggerFactory.getLogger(InntektsKomponentenService::class.java)

    override fun hentInntektListe(fnr: Foedselsnummer): InntektsKomponentenResponse {
        logger.info("Henter inntektliste fra inntektskomponenten")

        val hentInntektlisteRequest =
            HentInntektListeRequestBody(
                InntektListeIdent(fnr.value, "NATURLIG_IDENT"),
                "UfoereA-Inntekt",
                "2015-02",
                "2022-03",
                "Ufoere"
            )


        // Her må det muligens gjøres ett kall pr år. PGA tregheter mot eksterne systemer. Vi får bare teste
        val inntektsListe = runBlocking {
                inntektskomponentenClient.post<InntektsKomponentenResponse>("$url") {
                    contentType(ContentType.Application.Json)
                    body = hentInntektlisteRequest
                }
        }

        print(inntektsListe)

        return inntektsListe;

    }


}