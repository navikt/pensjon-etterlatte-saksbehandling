package no.nav.etterlatte.oppdrag

import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Endringskode
import no.nav.etterlatte.libs.common.vedtak.Enhetstype
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.trygdeetaten.skjema.oppdrag.Attestant180
import no.trygdeetaten.skjema.oppdrag.Avstemming115
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import no.trygdeetaten.skjema.oppdrag.Oppdrag110
import no.trygdeetaten.skjema.oppdrag.OppdragsEnhet120
import no.trygdeetaten.skjema.oppdrag.OppdragsLinje150
import no.trygdeetaten.skjema.oppdrag.TfradragTillegg
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.xml.datatype.DatatypeConstants
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

object OppdragMapper {

    private val tidspunktFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")

    fun oppdragFraVedtak(vedtak: Vedtak, attestasjon: Attestasjon, nokkelAvstemming: LocalDateTime): Oppdrag {
        val oppdrag110 = Oppdrag110().apply {
            kodeAksjon = "1"
            kodeEndring = "NY"
            kodeFagomraade = "BARNEPE"
            fagsystemId = vedtak.sakId
            utbetFrekvens = "MND"
            oppdragGjelderId = vedtak.sakIdGjelderFnr
            datoOppdragGjelderFom = LocalDate.parse("1900-01-01").toXMLDate()
            saksbehId = vedtak.saksbehandlerId

            avstemming115 = Avstemming115().apply {
                nokkelAvstemming = tidspunktFormatter.format(nokkelAvstemming)
                tidspktMelding = tidspunktFormatter.format(nokkelAvstemming)
                kodeKomponent = "BARNEPE"
            }

            vedtak.oppdragsenheter.forEach {
                oppdragsEnhet120.add(
                    OppdragsEnhet120().apply {
                        typeEnhet = when (it.enhetsType) {
                            Enhetstype.BOSTED -> "BOS"
                        }
                        enhet = "4819"
                        datoEnhetFom = LocalDate.parse("1900-01-01").toXMLDate()
                    }
                )
            }

            oppdragsLinje150.addAll(
                vedtak.beregningsperioder.map {
                    OppdragsLinje150().apply {
                        kodeEndringLinje = Endringskode.NY.toString()
                        vedtakId = vedtak.vedtakId
                        delytelseId = it.delytelsesId
                        kodeKlassifik = it.ytelseskomponent
                        datoVedtakFom = it.datoFOM.toXMLDate()
                        datoVedtakTom = it.datoTOM.toXMLDate()
                        sats = it.belop
                        fradragTillegg = TfradragTillegg.T
                        typeSats = "MND"
                        brukKjoreplan = "J"
                        saksbehId = vedtak.saksbehandlerId
                        utbetalesTilId = vedtak.sakIdGjelderFnr
                        henvisning = vedtak.behandlingsId

                        attestant180.add(
                            Attestant180().apply {
                                attestantId = attestasjon.attestantId
                            }
                        )
                    }
                }
            )
        }

        return Oppdrag().apply {
            this.oppdrag110 = oppdrag110
        }
    }

    private fun LocalDate.toXMLDate(): XMLGregorianCalendar {
        return DatatypeFactory.newInstance()
            .newXMLGregorianCalendar(GregorianCalendar.from(atStartOfDay(ZoneId.systemDefault()))).apply {
                timezone = DatatypeConstants.FIELD_UNDEFINED
            }
    }
}

fun Oppdrag.vedtakId() = oppdrag110.oppdragsLinje150.first().vedtakId