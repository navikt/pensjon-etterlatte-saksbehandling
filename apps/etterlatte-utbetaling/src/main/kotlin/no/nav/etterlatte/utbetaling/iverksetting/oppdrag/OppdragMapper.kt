package no.nav.etterlatte.utbetaling.iverksetting.oppdrag

import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.utbetaling.common.OppdragDefaults
import no.nav.etterlatte.utbetaling.common.OppdragslinjeDefaults
import no.nav.etterlatte.utbetaling.common.toXMLDate
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.OppdragKlassifikasjonskode
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetaling
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingslinjetype
import no.trygdeetaten.skjema.oppdrag.Attestant180
import no.trygdeetaten.skjema.oppdrag.Avstemming115
import no.trygdeetaten.skjema.oppdrag.LinjeTekst158
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import no.trygdeetaten.skjema.oppdrag.Oppdrag110
import no.trygdeetaten.skjema.oppdrag.OppdragsEnhet120
import no.trygdeetaten.skjema.oppdrag.OppdragsLinje150
import no.trygdeetaten.skjema.oppdrag.Tekst140
import no.trygdeetaten.skjema.oppdrag.TkodeStatusLinje
import java.time.format.DateTimeFormatter

object OppdragMapper {
    private val tidspunktFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")

    fun oppdragFraUtbetaling(
        utbetaling: Utbetaling,
        erFoersteUtbetalingPaaSak: Boolean,
        erGRegulering: Boolean,
    ): Oppdrag {
        val oppdrag110 =
            Oppdrag110().apply {
                kodeAksjon = OppdragDefaults.AKSJONSKODE_OPPDATER
                kodeEndring = if (erFoersteUtbetalingPaaSak) "NY" else "ENDR"
                kodeFagomraade = utbetaling.sakType.tilKodeFagomraade()
                fagsystemId = utbetaling.sakId.value.toString()
                utbetFrekvens = OppdragDefaults.UTBETALINGSFREKVENS
                oppdragGjelderId = utbetaling.stoenadsmottaker.value
                datoOppdragGjelderFom = OppdragDefaults.DATO_OPPDRAG_GJELDER_FOM
                saksbehId = utbetaling.saksbehandler.value

                avstemming115 =
                    Avstemming115().apply {
                        nokkelAvstemming = utbetaling.avstemmingsnoekkel.toNorskTid().format(tidspunktFormatter)
                        tidspktMelding = utbetaling.avstemmingsnoekkel.toNorskTid().format(tidspunktFormatter)
                        kodeKomponent = OppdragDefaults.AVLEVERENDE_KOMPONENTKODE
                    }

                oppdragsEnhet120.add(
                    OppdragsEnhet120().apply {
                        typeEnhet = OppdragDefaults.OPPDRAGSENHET.typeEnhet
                        enhet = OppdragDefaults.OPPDRAGSENHET.enhet
                        datoEnhetFom = OppdragDefaults.OPPDRAGSENHET.datoEnhetFom
                    },
                )

                if (erGRegulering) {
                    // TODO Denne eller LinjeTekst158?
                    tekst140.add(
                        Tekst140().apply {
                            // TODO Finne nødvendige felter og riktige verdier
                            tekst = "Hva skal teksten være her?"
                            datoTekstFom = utbetaling.utbetalingslinjer.first().periode.fra.toXMLDate()
                            datoTekstTom = utbetaling.utbetalingslinjer.first().periode.fra.plusDays(7).toXMLDate()
                        },
                    )
                }

                oppdragsLinje150.addAll(
                    utbetaling.utbetalingslinjer.map {
                        OppdragsLinje150().apply {
                            kodeEndringLinje = "NY"
                            if (it.erstatterId != null) {
                                refFagsystemId = utbetaling.sakId.value.toString()
                                refDelytelseId = it.erstatterId.value.toString()
                            }
                            when (it.type) {
                                Utbetalingslinjetype.OPPHOER -> {
                                    kodeStatusLinje = TkodeStatusLinje.OPPH
                                    datoStatusFom = it.periode.fra.toXMLDate()
                                }
                                else -> {}
                            }

                            vedtakId = utbetaling.vedtakId.value.toString()
                            delytelseId = it.id.value.toString()
                            kodeKlassifik = utbetaling.sakType.tilKodeklassifikasjon()
                            datoVedtakFom = it.periode.fra.toXMLDate()
                            datoVedtakTom = it.periode.til?.toXMLDate()
                            sats = it.beloep
                            fradragTillegg = OppdragslinjeDefaults.FRADRAG_ELLER_TILLEGG
                            typeSats = OppdragslinjeDefaults.UTBETALINGSFREKVENS
                            brukKjoreplan = it.kjoereplan.toString()
                            saksbehId = utbetaling.saksbehandler.value
                            utbetalesTilId = utbetaling.stoenadsmottaker.value
                            henvisning = utbetaling.behandlingId.shortValue.value

                            if (erGRegulering) {
                                // TODO Denne eller Tekst140?
                                linjeTekst158.add(
                                    LinjeTekst158().apply {
                                        // TODO Finne nødvendige felter og riktige verdier
                                        tekst = "Hva skal teksten være her?"
                                        tekstKode = ""
                                        datoTekstFom = it.periode.fra.toXMLDate()
                                        datoTekstTom = it.periode.fra.plusDays(7).toXMLDate()
                                    },
                                )
                            }

                            attestant180.add(
                                Attestant180().apply {
                                    attestantId = utbetaling.attestant.value
                                },
                            )
                        }
                    },
                )
            }

        return Oppdrag().apply {
            this.oppdrag110 = oppdrag110
        }
    }
}

fun Saktype.tilKodeFagomraade(): String =
    when (this) {
        Saktype.BARNEPENSJON -> "BARNEPE"
        Saktype.OMSTILLINGSSTOENAD -> "OMSTILL"
    }

fun Saktype.tilKodeklassifikasjon(): String =
    when (this) {
        Saktype.BARNEPENSJON -> OppdragKlassifikasjonskode.BARNEPENSJON_OPTP.toString()
        Saktype.OMSTILLINGSSTOENAD -> OppdragKlassifikasjonskode.OMSTILLINGSTOENAD_OPTP.toString()
    }

fun Oppdrag.vedtakId() = oppdrag110.oppdragsLinje150.first().vedtakId.toLong()

fun Oppdrag.sakId() = oppdrag110.fagsystemId
