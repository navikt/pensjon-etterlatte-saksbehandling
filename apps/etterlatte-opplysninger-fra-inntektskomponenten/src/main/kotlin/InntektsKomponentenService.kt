package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import org.slf4j.LoggerFactory

class InntektsKomponentenService(private val inntektskomponentenClient: HttpClient, private val url: String) :
    InntektsKomponenten {
    private val logger = LoggerFactory.getLogger(InntektsKomponentenService::class.java)

    override fun hentInntektListe(fnr: Foedselsnummer) {
        logger.info("Henter inntektliste fra inntektskomponenten")

        val hentInntektlisteRequest =
            HentInntektListeRequestBody(
                InntektListeIdent("12345678918", "NATURLIG_IDENT"),
                "UfoereA-Inntekt",
                "2019-02",
                "2022-03",
                "Ufoere"
            )


        runBlocking {
            val inntektsListe =
                inntektskomponentenClient.post<Any>("$url/inntektskomponenten-ws/rs/api/v1/hentinntektliste") {
                    contentType(ContentType.Application.Json)
                    body = hentInntektlisteRequest
                }
            print(inntektsListe)
        }


    }


}