package no.nav.etterlatte.utbetaling.iverksetting.oppdrag

import no.nav.etterlatte.domene.vedtak.Vedtak
import no.nav.etterlatte.libs.common.vedtak.Endringskode
import no.nav.etterlatte.utbetaling.common.Tidspunkt
import no.nav.etterlatte.utbetaling.common.forsteDagIMaaneden
import no.nav.etterlatte.utbetaling.common.sisteDagIMaaneden
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
                            refDelytelseId = it.erstatterId
                        }
                        kodeStatusLinje = it.endring?.let {
                            when (it) {
                                Endring.OPPHOER -> TkodeStatusLinje.OPPH
                            }
                        }
                        vedtakId = utbetaling.vedtakId.value
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

    fun oppdragFraVedtak(vedtak: Vedtak, avstemmingNokkel: Tidspunkt, endringskode: Endringskode): Oppdrag {
        val oppdrag110 = Oppdrag110().apply {
            kodeAksjon = "1"
            kodeEndring = "NY"
            kodeFagomraade = "BARNEPE"
            fagsystemId = vedtak.sak.id.toString()
            utbetFrekvens = "MND"
            oppdragGjelderId = vedtak.sak.ident
            datoOppdragGjelderFom = LocalDate.parse("1900-01-01").toXMLDate()
            saksbehId = vedtak.vedtakFattet!!.ansvarligSaksbehandler // TODO: avklare om vedtakFattet må være nullable

            avstemming115 = Avstemming115().apply {
                nokkelAvstemming = avstemmingNokkel.toNorskTid().format(tidspunktFormatter)
                tidspktMelding = avstemmingNokkel.toNorskTid().format(tidspunktFormatter)
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
                vedtak.pensjonTilUtbetaling!!.map { // // TODO: avklare om pensjonTilUtbetaling må være nullable
                    OppdragsLinje150().apply {
                        kodeEndringLinje =
                            Endringskode.NY.toString() // må være avklart om det er en endring eller en ny linje først
                        vedtakId = vedtak.vedtakId.toString()
                        delytelseId = it.id.toString()
                        kodeKlassifik = "BARNEPENSJON-OPTP"
                        datoVedtakFom = forsteDagIMaaneden(it.periode.fom).toXMLDate()
                        datoVedtakTom = it.periode.tom?.let { sisteDagIMaaneden(it).toXMLDate() }
                        sats = it.beloep
                        fradragTillegg = TfradragTillegg.T
                        typeSats = "MND"
                        brukKjoreplan = "J"
                        saksbehId = vedtak.vedtakFattet!!.ansvarligSaksbehandler
                        utbetalesTilId = vedtak.sak.ident
                        henvisning = vedtak.behandling.id.toString()

                        attestant180.add(
                            Attestant180().apply {
                                attestantId = vedtak.attestasjon!!.attestant
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