package no.nav.etterlatte.utbetaling.iverksetting.oppdrag

import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Endringskode
import no.nav.etterlatte.libs.common.vedtak.Enhetstype
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.utbetaling.common.Tidspunkt
import no.nav.etterlatte.utbetaling.common.toNorskTid
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetaling
import no.trygdeetaten.skjema.oppdrag.Attestant180
import no.trygdeetaten.skjema.oppdrag.Avstemming115
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import no.trygdeetaten.skjema.oppdrag.Oppdrag110
import no.trygdeetaten.skjema.oppdrag.OppdragsEnhet120
import no.trygdeetaten.skjema.oppdrag.OppdragsLinje150
import no.trygdeetaten.skjema.oppdrag.TfradragTillegg
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.xml.datatype.DatatypeConstants
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

object OppdragMapper {

    private val tidspunktFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")

    fun oppdragFraUtbetaling(utbetaling: Utbetaling): Oppdrag {
        val oppdrag110 = Oppdrag110().apply {
            kodeAksjon = "1"
            kodeEndring = "NY"
            kodeFagomraade = "BARNEPE"
            fagsystemId = utbetaling.sakId.value
            utbetFrekvens = "MND"
            oppdragGjelderId = utbetaling.foedselsnummer.value
            datoOppdragGjelderFom = LocalDate.parse("1900-01-01").toXMLDate()
            saksbehId = vedtak.saksbehandlerId

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
                        kodeEndringLinje = Endringskode.NY.toString()
                        vedtakId = utbetaling.vedtakId.value
                        delytelseId = it.id.toString()
                        kodeKlassifik = "" // TODO
                        datoVedtakFom = it.periode.fom.toXMLDate()
                        datoVedtakTom = it.periode.tom.toXMLDate()
                        sats = it.beloep
                        fradragTillegg = TfradragTillegg.T
                        typeSats = "MND"
                        brukKjoreplan = "J"
                        saksbehId = vedtak.saksbehandlerId
                        utbetalesTilId = utbetaling.foedselsnummer.value
                        henvisning = utbetaling.behandlingId.value

                        attestant180.add(
                            Attestant180().apply {
                                attestantId = vedtak.attestasjon.attestantId
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