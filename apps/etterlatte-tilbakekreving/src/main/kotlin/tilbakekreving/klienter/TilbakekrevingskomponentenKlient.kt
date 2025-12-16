package no.nav.etterlatte.tilbakekreving.klienter

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
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tilbakekreving.KlasseType
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingAarsak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVedtak
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekrevingsbelop
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.tilbakekreving.TilbakekrevingHendelseRepository
import no.nav.etterlatte.tilbakekreving.TilbakekrevingHendelseType
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagMapper
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.toLocalDate
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentDetaljRequest
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentDetaljResponse
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakResponse
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.HentKravgrunnlagDetaljDto
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsbelopDto
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsperiodeDto
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsvedtakDto
import no.nav.tilbakekreving.typer.v1.PeriodeDto
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

class TilbakekrevingskomponentenKlient(
    private val url: String,
    private val httpClient: HttpClient,
    private val hendelseRepository: TilbakekrevingHendelseRepository,
) {
    // Egen objectmapper for å fjerne timestamp fra xml-datoer da dette ikke blir riktig mot tilbakekrevingskomponenten
    private val tilbakekrevingObjectMapper: ObjectMapper =
        objectMapper.copy().registerModule(
            CustomXMLGregorianCalendarModule(),
        )

    private val logger = LoggerFactory.getLogger(javaClass)
    private val sikkerLogg = sikkerlogger()

    fun sendTilbakekrevingsvedtak(vedtak: TilbakekrevingVedtak) {
        logger.info("Sender tilbakekrevingsvedtak ${vedtak.vedtakId} til tilbakekrevingskomponenten")
        val request = toTilbakekrevingsvedtakRequest(vedtak)
        val requestAsJson = tilbakekrevingObjectMapper.writeValueAsString(request)

        hendelseRepository.lagreTilbakekrevingHendelse(
            sakId = vedtak.sakId,
            payload = requestAsJson,
            type = TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_SENDT,
        )

        val response =
            runBlocking {
                val httpResponse =
                    httpClient.post("$url/tilbakekreving/tilbakekrevingsvedtak") {
                        contentType(ContentType.Application.Json)
                        setBody(requestAsJson)
                    }

                httpResponse.body<TilbakekrevingsvedtakResponse>()
            }

        hendelseRepository.lagreTilbakekrevingHendelse(
            sakId = vedtak.sakId,
            payload = tilbakekrevingObjectMapper.writeValueAsString(response),
            type = TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_KVITTERING,
        )

        return kontrollerResponse(response)
    }

    fun hentKravgrunnlag(
        sakId: SakId,
        kravgrunnlagId: Long,
    ): Kravgrunnlag? {
        logger.info(
            "Henter kravgrunnlag for tilbakekreving på sak $sakId med kravgrunnlagId $kravgrunnlagId " +
                "fra tilbakekrevingskomponenten",
        )
        val request = toKravgrunnlagHentDetaljRequest(kravgrunnlagId)
        val requestAsJson = tilbakekrevingObjectMapper.writeValueAsString(request)

        hendelseRepository.lagreTilbakekrevingHendelse(
            sakId = sakId,
            payload = requestAsJson,
            type = TilbakekrevingHendelseType.KRAVGRUNNLAG_FORESPOERSEL_SENDT,
        )

        val response =
            runBlocking {
                val httpResponse =
                    httpClient.post("$url/tilbakekreving/kravgrunnlag") {
                        contentType(ContentType.Application.Json)
                        setBody(requestAsJson)
                    }

                httpResponse.body<KravgrunnlagHentDetaljResponse>()
            }

        hendelseRepository.lagreTilbakekrevingHendelse(
            sakId = sakId,
            payload = tilbakekrevingObjectMapper.writeValueAsString(response),
            type = TilbakekrevingHendelseType.KRAVGRUNNLAG_FORESPOERSEL_KVITTERING,
        )

        if (response.detaljertkravgrunnlag == null) {
            return null
        }
        val kravgrunnlag = KravgrunnlagMapper.toKravgrunnlag(response.detaljertkravgrunnlag)

        return kravgrunnlag
    }

    private fun toTilbakekrevingsvedtakRequest(vedtak: TilbakekrevingVedtak): TilbakekrevingsvedtakRequest =
        TilbakekrevingsvedtakRequest().apply {
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
                                belopRenter =
                                    tilbakekrevingPeriode.tilbakekrevingsbeloep
                                        .filter { it.klasseType == KlasseType.YTEL.name }
                                        .sumOf { it.rentetillegg ?: 0 }
                                        .medToDesimaler()

                                tilbakekrevingsbelop.apply {
                                    // Mapper YTEL (det saksbehandler har svart ut)
                                    tilbakekrevingPeriode.tilbakekrevingsbeloep
                                        .filter {
                                            it.klasseType == KlasseType.YTEL.name
                                        }.forEach {
                                            add(
                                                it.toTilbakekrevingsbelopYtelse(
                                                    vedtak.aarsak,
                                                    vedtak.overstyrBehandletNettoTilBruttoMotTilbakekreving,
                                                ),
                                            )
                                        }

                                    // Andre klassetyper, blant annet FEIL
                                    tilbakekrevingPeriode.tilbakekrevingsbeloep
                                        .filter {
                                            it.klasseType != KlasseType.YTEL.name
                                        }.forEach { add(it.toTilbakekreivngsbelopAndreKlassetyper()) }
                                }
                            }
                        },
                    )
                }
        }

    private fun Tilbakekrevingsbelop.toTilbakekrevingsbelopYtelse(
        aarsak: TilbakekrevingAarsak,
        overstyrBehandletNettoTilBrutto: Boolean,
    ): TilbakekrevingsbelopDto {
        val utenOverstyring =
            TilbakekrevingsbelopDto().apply {
                kodeKlasse = klasseKode
                belopOpprUtbet = bruttoUtbetaling.medToDesimaler()
                belopNy = nyBruttoUtbetaling.medToDesimaler()
                belopTilbakekreves = krevIkkeNull(bruttoTilbakekreving?.medToDesimaler()) { "Tilbakekrevingsbeløp mangler" }
                belopSkatt = krevIkkeNull(skatt?.medToDesimaler()) { "Skattebeløp mangler" }
                kodeResultat = krevIkkeNull(resultat?.name) { "Resultatkode mangler" }
                kodeAarsak = mapFraTilbakekrevingAarsak(aarsak)
                kodeSkyld = krevIkkeNull(skyld?.name) { "Skyldkode mangler" }
            }
        if (overstyrBehandletNettoTilBrutto) {
            logger.info("Overstyrer opprinnelig tilbakekrevingsbeløp, med å sette beløp tilbakekreves = netto, og skattebeløp til 0")
            return utenOverstyring.apply {
                belopTilbakekreves = nettoTilbakekreving!!.medToDesimaler()
                belopSkatt = BigDecimal.ZERO
            }
        } else {
            return utenOverstyring
        }
    }

    private fun mapFraTilbakekrevingAarsak(aarsak: TilbakekrevingAarsak): String =
        when (aarsak) {
            TilbakekrevingAarsak.UTBFEILMOT -> aarsak.name
            else -> TilbakekrevingAarsak.ANNET.name
        }

    private fun Tilbakekrevingsbelop.toTilbakekreivngsbelopAndreKlassetyper() =
        TilbakekrevingsbelopDto().apply {
            // Kun obligatoriske felter sendes her
            kodeKlasse = klasseKode
            belopOpprUtbet = bruttoUtbetaling.medToDesimaler()
            belopNy = nyBruttoUtbetaling.medToDesimaler()
            belopTilbakekreves = krevIkkeNull(bruttoTilbakekreving?.medToDesimaler()) { "Tilbakekrevingsbeløp mangler" }
        }

    private fun toKravgrunnlagHentDetaljRequest(kravgrunnlagId: Long): KravgrunnlagHentDetaljRequest =
        KravgrunnlagHentDetaljRequest().apply {
            hentkravgrunnlag =
                HentKravgrunnlagDetaljDto().apply {
                    // Hent kravgrunnlag for danning av nytt tilbakekrevingsvedtak
                    kodeAksjon = "4"
                    this.kravgrunnlagId = kravgrunnlagId.toBigInteger()
                    saksbehId = INTERN_SAKSBEHANDLER
                    enhetAnsvarlig = ANSVARLIG_ENHET
                }
        }

    private fun kontrollerResponse(response: TilbakekrevingsvedtakResponse) =
        when (val alvorlighetsgrad = Alvorlighetsgrad.fromString(response.mmel.alvorlighetsgrad)) {
            Alvorlighetsgrad.OK,
            Alvorlighetsgrad.OK_MED_VARSEL,
            -> Unit

            Alvorlighetsgrad.ALVORLIG_FEIL,
            Alvorlighetsgrad.SQL_FEIL,
            -> {
                val err = "Tilbakekrevingsvedtak feilet med alvorlighetsgrad $alvorlighetsgrad"
                sikkerLogg.error(err, kv("response", response.toJson()))
                response.mmel.beskrMelding
                throw InternfeilException(err)
            }
        }

    enum class Alvorlighetsgrad(
        val value: String,
    ) {
        OK("00"),

        /** En varselmelding følger med */
        OK_MED_VARSEL("04"),

        /** Alvorlig feil som logges og stopper behandling av aktuelt tilfelle*/
        ALVORLIG_FEIL("08"),
        SQL_FEIL("12"),
        ;

        override fun toString() = value

        companion object {
            fun fromString(string: String): Alvorlighetsgrad = enumValues<Alvorlighetsgrad>().first { it.value == string }
        }
    }

    private enum class KodeAksjon(
        val kode: String,
    ) {
        FATTE_VEDTAK("8"),
    }

    private enum class RenterBeregnes(
        val kode: String,
    ) {
        NEI("N"),
    }

    private companion object {
        const val ANSVARLIG_ENHET = "4819"
        const val INTERN_SAKSBEHANDLER = "EY"
    }

    private fun LocalDate.toXMLDate(): XMLGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(toString())

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
                        gen?.writeString(
                            value
                                .toLocalDate()
                                .toString(),
                        )
                    }
                }
            },
        )
    }
}
