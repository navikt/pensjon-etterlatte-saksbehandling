package no.nav.etterlatte.tilbakekreving.vedtak

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.objectMapper
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
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

class TilbakekrevingKlient(
    private val url: String,
    private val httpClient: HttpClient,
    private val hendelseRepository: TilbakekrevingHendelseRepository,
) {
    // Egen objectmapper for å fjerne timestamp fra xml-datoer da dette ikke blir riktig mot tilbakekrevingskomponenten
    private val vedtakObjectMapper: ObjectMapper = objectMapper.copy().registerModule(CustomXMLGregorianCalendarModule())

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

                    // Dette er vedtaksid'en fra kravgrunnlaget, altså ikke vedtaksid'en fra vedtaket i Gjenny
                    vedtakId = vedtak.vedtakId.toBigInteger()
                    datoVedtakFagsystem = vedtak.fattetVedtak.dato.toXMLDate()

                    // Settes kun dersom det skal beregnes renter på hele tilbakekrevingen av tilbakekrevingskomponenten
                    renterBeregnes = RenterBeregnes.NEI.kode
                    saksbehId = vedtak.fattetVedtak.saksbehandler

                    // Skal være satt som statisk enhet 4819
                    enhetAnsvarlig = ANSVARLIG_ENHET
                    kodeHjemmel = vedtak.hjemmel.kode

                    // Dette skal være likt kontrollfeltet i mottatt kravgrunnlag, hvis forskjellig er grunnlag utdatert
                    kontrollfelt = vedtak.kontrollfelt
                    tilbakekrevingsperiode.addAll(
                        vedtak.perioder.map { tilbakekrevingPeriode ->
                            TilbakekrevingsperiodeDto().apply {
                                periode =
                                    PeriodeDto().apply {
                                        fom = tilbakekrevingPeriode.maaned.atDay(1).toXMLDate()
                                        tom = tilbakekrevingPeriode.maaned.atEndOfMonth().toXMLDate()
                                    }

                                // Saksbehandler beregner renter, derfor settes denne til NEI
                                renterBeregnes = RenterBeregnes.NEI.kode
                                belopRenter = tilbakekrevingPeriode.ytelse.rentetillegg.medToDesimaler()

                                tilbakekrevingsbelop.apply {
                                    add(tilbakekrevingPeriode.ytelse.toTilbakekreivngsbelopYtelse(vedtak.aarsak))

                                    // Feilkonto skal i praksis være tilsvarende det vi mottar
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
            belopOpprUtbet = bruttoUtbetaling.medToDesimaler()
            belopNy = nyBruttoUtbetaling.medToDesimaler()
            belopTilbakekreves = bruttoTilbakekreving.medToDesimaler()
            belopSkatt = skatt.medToDesimaler()
            kodeResultat = resultat.name
            kodeAarsak = mapFraTilbakekrevingAarsak(aarsak)
            kodeSkyld = skyld.name
        }

    private fun mapFraTilbakekrevingAarsak(aarsak: TilbakekrevingAarsak): String {
        return when (aarsak) {
            TilbakekrevingAarsak.UTBFEILMOT -> aarsak.name
            else -> TilbakekrevingAarsak.ANNET.name
        }
    }

    private fun TilbakekrevingsbelopFeilkontoVedtak.toTilbakekreivngsbelopFeilkonto() =
        TilbakekrevingsbelopDto().apply {
            // Kun obligatoriske felter sendes her
            kodeKlasse = klasseKode
            belopOpprUtbet = bruttoUtbetaling.medToDesimaler()
            belopNy = nyBruttoUtbetaling.medToDesimaler()
            belopTilbakekreves = bruttoTilbakekreving.medToDesimaler()
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
        NEI("N"),
    }

    private companion object {
        const val ANSVARLIG_ENHET = "4819"
    }

    private fun LocalDate.toXMLDate(): XMLGregorianCalendar {
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(toString())
    }

    private fun Int.medToDesimaler() = this.toBigDecimal().setScale(2)
}

private class CustomXMLGregorianCalendarModule : SimpleModule() {
    init {
        addSerializer(
            XMLGregorianCalendar::class.java,
            object : JsonSerializer<XMLGregorianCalendar>() {
                override fun serialize(
                    value: XMLGregorianCalendar?,
                    gen: JsonGenerator?,
                    ser: SerializerProvider?,
                ) {
                    if (value != null) {
                        gen?.writeString(value.toGregorianCalendar().toZonedDateTime().toLocalDate().toString())
                    }
                }
            },
        )
    }
}
