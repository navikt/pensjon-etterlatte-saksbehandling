package no.nav.etterlatte.tilbakekreving.vedtak

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingAarsak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVedtak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingsbelopFeilkontoVedtak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingsbelopYtelseVedtak
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.tilbakekreving.hendelse.TilbakekrevingHendelseRepository
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakResponse
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsbelopDto
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsperiodeDto
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsvedtakDto
import no.nav.tilbakekreving.typer.v1.PeriodeDto
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.xml.datatype.DatatypeConstants
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

class TilbakekrevingKlient(
    private val url: String,
    private val httpClient: HttpClient,
    private val hendelseRepository: TilbakekrevingHendelseRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val sikkerLogg = sikkerlogger()

    fun sendTilbakekrevingsvedtak(vedtak: TilbakekrevingVedtak) {
        logger.info("Sender tilbakekrevingsvedtak ${vedtak.vedtakId} til tilbakekrevingskomponenten")
        val request = toTilbakekrevingsvedtakRequest(vedtak)
        val requestAsJson = vedtakObjectMapper.writeValueAsString(request)

        hendelseRepository.lagreTilbakekrevingsvedtakSendt(vedtak.kravgrunnlagId, requestAsJson)

        val response =
            runBlocking {
                val httpResponse =
                    httpClient.post("$url/tilbakekreving/tilbakekrevingsvedtak") {
                        contentType(ContentType.Application.Json)
                        setBody(requestAsJson)
                    }

                httpResponse.body<TilbakekrevingsvedtakResponse>()
            }

        hendelseRepository.lagreTilbakekrevingsvedtakKvitteringMottatt(vedtak.kravgrunnlagId, response.toJson())

        return kontrollerResponse(response)
    }

    private fun toTilbakekrevingsvedtakRequest(vedtak: TilbakekrevingVedtak): TilbakekrevingsvedtakRequest {
        return TilbakekrevingsvedtakRequest().apply {
            tilbakekrevingsvedtak =
                TilbakekrevingsvedtakDto().apply {
                    kodeAksjon = KodeAksjon.FATTE_VEDTAK.kode
                    vedtakId = vedtak.vedtakId.toBigInteger()
                    datoVedtakFagsystem = vedtak.fattetVedtak.dato.toXMLDate()
                    renterBeregnes = RenterBeregnes.NEI.kode
                    saksbehId = vedtak.fattetVedtak.saksbehandler
                    enhetAnsvarlig = ANSVARLIG_ENHET
                    kodeHjemmel = vedtak.hjemmel.kode
                    kontrollfelt = vedtak.kontrollfelt
                    tilbakekrevingsperiode.addAll(
                        vedtak.perioder.map { tilbakekrevingPeriode ->
                            TilbakekrevingsperiodeDto().apply {
                                periode =
                                    PeriodeDto().apply {
                                        fom = tilbakekrevingPeriode.maaned.atDay(1).toXMLDate()
                                        tom = tilbakekrevingPeriode.maaned.atEndOfMonth().toXMLDate()
                                    }
                                renterBeregnes =
                                    if (tilbakekrevingPeriode.ytelse.rentetillegg > 0) {
                                        RenterBeregnes.JA.kode
                                    } else {
                                        RenterBeregnes.NEI.kode
                                    }
                                belopRenter = tilbakekrevingPeriode.ytelse.rentetillegg.toBigDecimal()

                                tilbakekrevingsbelop.apply {
                                    add(tilbakekrevingPeriode.ytelse.toTilbakekreivngsbelopYtelse(vedtak.aarsak))
                                    add(tilbakekrevingPeriode.feilkonto.toTilbakekreivngsbelopFeilkonto())
                                }
                            }
                        },
                    )
                }
        }
    }

    private fun TilbakekrevingsbelopYtelseVedtak.toTilbakekreivngsbelopYtelse(aarsak: TilbakekrevingAarsak) =
        TilbakekrevingsbelopDto().apply {
            kodeKlasse = klasseKode
            belopOpprUtbet = bruttoUtbetaling.toBigDecimal()
            belopNy = nyBruttoUtbetaling.toBigDecimal()
            belopTilbakekreves = bruttoTilbakekreving.toBigDecimal()
            belopSkatt = skatt.toBigDecimal()
            kodeResultat = resultat.name
            kodeAarsak = aarsak.name
            kodeSkyld = skyld.name
        }

    private fun TilbakekrevingsbelopFeilkontoVedtak.toTilbakekreivngsbelopFeilkonto() =
        TilbakekrevingsbelopDto().apply {
            kodeKlasse = klasseKode
            belopOpprUtbet = bruttoUtbetaling.toBigDecimal().setScale(2)
            belopNy = nyBruttoUtbetaling.toBigDecimal().setScale(2)
            belopTilbakekreves = bruttoTilbakekreving.toBigDecimal().setScale(2)
        }

    private fun kontrollerResponse(response: TilbakekrevingsvedtakResponse) {
        return when (val alvorlighetsgrad = Alvorlighetsgrad.fromString(response.mmel.alvorlighetsgrad)) {
            Alvorlighetsgrad.OK,
            Alvorlighetsgrad.OK_MED_VARSEL,
            -> Unit

            Alvorlighetsgrad.ALVORLIG_FEIL,
            Alvorlighetsgrad.SQL_FEIL,
            -> {
                val err = "Tilbakekrevingsvedtak feilet med alvorlighetsgrad $alvorlighetsgrad"
                sikkerLogg.error(err, kv("response", response.toJson()))
                throw Exception(err)
            }
        }
    }

    private fun LocalDate.toXMLDate(): XMLGregorianCalendar {
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(
            LocalDateTime.of(this, LocalTime.MIDNIGHT).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
        ).apply {
            timezone = DatatypeConstants.FIELD_UNDEFINED
        }
    }

    enum class Alvorlighetsgrad(val value: String) {
        OK("00"),

        /** En varselmelding følger med */
        OK_MED_VARSEL("04"),

        /** Alvorlig feil som logges og stopper behandling av aktuelt tilfelle*/
        ALVORLIG_FEIL("08"),
        SQL_FEIL("12"),
        ;

        override fun toString() = value

        companion object {
            fun fromString(string: String): Alvorlighetsgrad {
                return enumValues<Alvorlighetsgrad>().first { it.value == string }
            }
        }
    }

    private enum class KodeAksjon(val kode: String) {
        FATTE_VEDTAK("8"),
    }

    private enum class RenterBeregnes(val kode: String) {
        JA("J"),
        NEI("N"),
    }

    private companion object {
        const val ANSVARLIG_ENHET = "4819"
    }
}

// Brukes for å få riktig dato-format ved serialisering
private val vedtakObjectMapper: ObjectMapper =
    JsonMapper.builder()
        .addModule(JavaTimeModule())
        .addModule(KotlinModule())
        .addModule(CustomXMLGregorianCalendarModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
        .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
        .build()

private class CustomXMLGregorianCalendarModule : SimpleModule() {
    init {
        addSerializer(
            XMLGregorianCalendar::class.java,
            object : JsonSerializer<XMLGregorianCalendar>() {
                override fun serialize(
                    value: XMLGregorianCalendar?,
                    gen: JsonGenerator?,
                    serializers: SerializerProvider?,
                ) {
                    if (value != null) {
                        gen?.writeString(value.toGregorianCalendar().toZonedDateTime().toLocalDate().toString())
                    }
                }
            },
        )
    }
}
