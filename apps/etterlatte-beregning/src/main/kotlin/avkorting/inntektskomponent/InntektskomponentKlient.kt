package no.nav.etterlatte.avkorting.inntektskomponent

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.toJson
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth

class InntektskomponentKlient(
    val httpClient: HttpClient,
    val url: String,
) {
    private val logger = LoggerFactory.getLogger(InntektskomponentKlient::class.java)
    private val sikkerlogg = sikkerlogger()

    companion object {
        const val ETTERLATTEYTELSER = "Etterlatteytelser"
    }

    suspend fun hentInntekt(
        personident: String,
        maanedFom: YearMonth,
        maanedTom: YearMonth,
    ): AInntektReponsData =
        retry {
            httpClient.post("$url/rest/v2/inntekt") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                val body =
                    InntektRequest(
                        personident = personident,
                        filter = ETTERLATTEYTELSER,
                        formaal = ETTERLATTEYTELSER,
                        maanedFom = maanedFom.toString(),
                        maanedTom = maanedTom.toString(),
                    ).toJson()
                setBody(body)
            }
        }.let {
            when (it) {
                is RetryResult.Success -> {
                    it.content.body<AInntektReponsData>()
                }

                is RetryResult.Failure -> {
                    logger.error("Kall mot inntektskomponent feilet")
                    sikkerlogg.error("Kall mot inntektskomponent feilet for $personident")
                    throw it.samlaExceptions()
                }
            }
        }
}

data class InntektRequest(
    val personident: String,
    val filter: String,
    val formaal: String,
    val maanedFom: String,
    val maanedTom: String,
)

data class AInntektReponsData(
    val data: List<InntektsinformasjonDto>,
)

data class InntektsinformasjonDto(
    val maaned: String,
    val opplysningspliktig: String,
    val underenhet: String,
    val norskident: String,
    val oppsummeringstidspunkt: OffsetDateTime,
    val inntektListe: List<InntektDto>,
)

data class InntektDto(
    val type: String,
    val beloep: BigDecimal,
    val fordel: String,
    val beskrivelse: String,
    val inngaarIGrunnlagForTrekk: Boolean,
    val utloeserArbeidsgiveravgift: Boolean,
    val opptjeningsperiodeFom: LocalDate,
    val opptjeningsperiodeTom: LocalDate,
)
