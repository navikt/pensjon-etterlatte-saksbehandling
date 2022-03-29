package no.nav.etterlatte.vedtaksoversetter

import no.nav.etterlatte.domain.Endringskode
import no.nav.etterlatte.domain.Enhetstype
import no.nav.etterlatte.domain.Vedtak
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import no.trygdeetaten.skjema.oppdrag.Oppdrag110
import no.trygdeetaten.skjema.oppdrag.OppdragsEnhet120
import no.trygdeetaten.skjema.oppdrag.OppdragsLinje150
import no.trygdeetaten.skjema.oppdrag.TfradragTillegg
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.xml.datatype.DatatypeConstants
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

object OppdragMapper {

    fun oppdragFraVedtak(vedtak: Vedtak): Oppdrag {
        val oppdrag110 = Oppdrag110().apply {
            kodeAksjon = "1" // 3 = simulering
            kodeEndring = "NY" // Alltid NY for førstegangsinnvilgelse
            kodeFagomraade = "EY" // TODO må legges inn hos økonomi
            fagsystemId = vedtak.sakId
            utbetFrekvens = "MND"
            oppdragGjelderId = vedtak.sakIdGjelderFnr
            datoOppdragGjelderFom = vedtak.aktorFoedselsdato.toXMLDate()
            saksbehId = vedtak.saksbehandlerId

            // aktuelle enheter knyttet til oppdraget
            vedtak.oppdragsenheter.forEach {
                oppdragsEnhet120.add(
                    OppdragsEnhet120().apply {
                        typeEnhet = when (it.enhetsType) {
                            Enhetstype.BOSTED -> "BOS"
                        }
                        enhet = it.enhetsnummer
                        datoEnhetFom = it.datoEnhetFOM.toXMLDate()
                    }
                )
            }

            oppdragsLinje150.addAll(
                vedtak.beregningsperioder.map {
                    OppdragsLinje150().apply {
                        kodeEndringLinje = Endringskode.NY.toString()
                        //kodeStatusLinje
                        //datoStatusFom
                        vedtakId = vedtak.vedtakId
                        delytelseId = it.delytelsesId
                        //linjeid // får dette fra Oppdragssystemet
                        kodeKlassifik = it.ytelseskomponent.toString()
                        //datoKlassifikFom
                        datoVedtakFom = it.datoFOM.toXMLDate()
                        datoVedtakTom =
                            it.datoTOM.toXMLDate() // TODO sjekk med vedtak om siste i mnd eller første i neste mnd

                        sats = it.belop
                        fradragTillegg = TfradragTillegg.T
                        typeSats = "MND"
                        //skyldnerId
                        //datoSkyldnerFom
                        //kravhaverId // TODO: usikker på denne
                        //datoKravhaverFom
                        //kid
                        //datoKidFom
                        brukKjoreplan = "J" // TODO: sjekk om "J" stemmer
                        saksbehId = vedtak.saksbehandlerId
                        utbetalesTilId = vedtak.sakIdGjelderFnr
                        //datoUtbetalesTilIdFom
                        //kodeArbeidsgiver
                        henvisning = vedtak.behandlingsId
                        //typeSoknad = "" TODO: tror det skal stå noe her
                        //refFagsystemId
                        //refOppdragsId
                        //refDelytelseId
                        //refLinjeId
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