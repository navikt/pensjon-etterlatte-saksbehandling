package no.nav.etterlatte.utbetaling.iverksetting.oppdrag

import no.nav.etterlatte.utbetaling.common.toNorskTid
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Endring
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetaling
import no.trygdeetaten.skjema.oppdrag.Attestant180
import no.trygdeetaten.skjema.oppdrag.Avstemming115
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import no.trygdeetaten.skjema.oppdrag.Oppdrag110
import no.trygdeetaten.skjema.oppdrag.OppdragsEnhet120
import no.trygdeetaten.skjema.oppdrag.OppdragsLinje150
import no.trygdeetaten.skjema.oppdrag.TfradragTillegg
import no.trygdeetaten.skjema.oppdrag.TkodeStatusLinje
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.xml.datatype.DatatypeConstants
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar


object OppdragMapper {

    private val tidspunktFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")

    fun oppdragFraUtbetaling(utbetaling: Utbetaling, foerstegangsinnvilgelse: Boolean): Oppdrag {
        val oppdrag110 = Oppdrag110().apply {
            kodeAksjon = "1"
            kodeEndring = if (foerstegangsinnvilgelse) "NY" else "ENDR"
            kodeFagomraade = "BARNEPE"
            fagsystemId = utbetaling.sakId.value.toString()
            utbetFrekvens = "MND"
            oppdragGjelderId = utbetaling.stoenadsmottaker.value
            datoOppdragGjelderFom = LocalDate.parse("1900-01-01").toXMLDate()
            saksbehId = utbetaling.saksbehandler.value

            avstemming115 = Avstemming115().apply {
                nokkelAvstemming = utbetaling.avstemmingsnoekkel.toNorskTid().format(tidspunktFormatter)
                tidspktMelding = utbetaling.avstemmingsnoekkel.toNorskTid().format(tidspunktFormatter)
                kodeKomponent = "ETTERLAT"
            }

            oppdragsEnhet120.add(
                OppdragsEnhet120().apply {
                    typeEnhet = "BOS"
                    enhet = "4819"
                    datoEnhetFom = LocalDate.parse("1900-01-01").toXMLDate()
                }
            )

            oppdragsLinje150.addAll(
                utbetaling.utbetalingslinjer.map {
                    OppdragsLinje150().apply {
                        if (it.endring == null) {
                            kodeEndringLinje = "NY"
                        } else {
                            kodeEndringLinje = "ENDR"
                            refFagsystemId = utbetaling.sakId.value.toString()
                            refDelytelseId = it.erstatterId?.value.toString()
                        }
                        kodeStatusLinje = it.endring?.let {
                            when (it) {
                                Endring.OPPHOER -> TkodeStatusLinje.OPPH
                            }
                        }
                        vedtakId = utbetaling.vedtakId.value.toString()
                        delytelseId = it.id.value.toString()
                        kodeKlassifik = "BARNEPENSJON-OPTP"
                        datoVedtakFom = it.periode.fra.toXMLDate()
                        datoVedtakTom = it.periode.til?.let { it.toXMLDate() }
                        sats = it.beloep
                        fradragTillegg = TfradragTillegg.T
                        typeSats = "MND"
                        brukKjoreplan = "J"
                        saksbehId = utbetaling.saksbehandler.value
                        utbetalesTilId = utbetaling.stoenadsmottaker.value
                        henvisning = utbetaling.behandlingId.value

                        attestant180.add(
                            Attestant180().apply {
                                attestantId = utbetaling.attestant.value
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

fun Oppdrag.vedtakId() = oppdrag110.oppdragsLinje150.first().vedtakId.toLong()