package no.nav.etterlatte.oppdrag

import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Endringskode
import no.nav.etterlatte.libs.common.vedtak.Enhetstype
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.trygdeetaten.skjema.oppdrag.Attestant180
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

    fun oppdragFraVedtak(vedtak: Vedtak, attestasjon: Attestasjon): Oppdrag {
        val oppdrag110 = Oppdrag110().apply {
            kodeAksjon = "1" // 3 = simulering
            kodeEndring = "NY" // Alltid NY for førstegangsinnvilgelse
            kodeFagomraade = "PENBP" // TODO midlertidig verdi fra pesys
            fagsystemId = vedtak.sakId
            utbetFrekvens = "MND"
            oppdragGjelderId = vedtak.sakIdGjelderFnr
            datoOppdragGjelderFom =
                LocalDate.parse("1900-01-01").toXMLDate() // TODO første virkningsdato eller 01.01.1900
            saksbehId = vedtak.saksbehandlerId

            // aktuelle enheter knyttet til oppdraget
            vedtak.oppdragsenheter.forEach {
                oppdragsEnhet120.add(
                    OppdragsEnhet120().apply {
                        typeEnhet = when (it.enhetsType) {
                            Enhetstype.BOSTED -> "BOS"
                        }
                        enhet = "4819" // alltid 4819 enhetsnummer (ikke enheten som fatter vedtaket)
                        datoEnhetFom = LocalDate.parse("1900-01-01").toXMLDate()// TODO 01.01.1900
                    }
                )
            }

            oppdragsLinje150.addAll(
                vedtak.beregningsperioder.map {
                    OppdragsLinje150().apply {
                        kodeEndringLinje = Endringskode.NY.toString()
                        //kodeStatusLinje
                        //datoStatusFom TODO ved opphør skal denne være fra første mnd etter
                        vedtakId = vedtak.vedtakId
                        delytelseId =
                            it.delytelsesId // TODO: kan være hva som helst - må være unik innenfor Oppdrag - må taes vare på
                        //linjeid // får dette fra Oppdragssystemet
                        kodeKlassifik = it.ytelseskomponent.toString() // PENBPGP-OPTP
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