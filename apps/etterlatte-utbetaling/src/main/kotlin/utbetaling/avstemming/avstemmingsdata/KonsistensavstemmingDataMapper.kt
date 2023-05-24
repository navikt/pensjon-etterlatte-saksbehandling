package no.nav.etterlatte.utbetaling.avstemming.avstemmingsdata

import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.utbetaling.avstemming.Konsistensavstemming
import no.nav.etterlatte.utbetaling.avstemming.OppdragForKonsistensavstemming
import no.nav.etterlatte.utbetaling.common.ANTALL_DETALJER_PER_AVSTEMMINGMELDING_OPPDRAG
import no.nav.etterlatte.utbetaling.common.OppdragDefaults
import no.nav.etterlatte.utbetaling.common.OppdragslinjeDefaults
import no.nav.etterlatte.utbetaling.common.tidsstempelDatoOppdrag
import no.nav.etterlatte.utbetaling.common.tidsstempelMikroOppdrag
import no.nav.virksomhet.tjenester.avstemming.informasjon.konsistensavstemmingsdata.v1.Aksjonsdata
import no.nav.virksomhet.tjenester.avstemming.informasjon.konsistensavstemmingsdata.v1.Attestant
import no.nav.virksomhet.tjenester.avstemming.informasjon.konsistensavstemmingsdata.v1.Enhet
import no.nav.virksomhet.tjenester.avstemming.informasjon.konsistensavstemmingsdata.v1.Konsistensavstemmingsdata
import no.nav.virksomhet.tjenester.avstemming.informasjon.konsistensavstemmingsdata.v1.Oppdragsdata
import no.nav.virksomhet.tjenester.avstemming.informasjon.konsistensavstemmingsdata.v1.Oppdragslinje
import no.nav.virksomhet.tjenester.avstemming.informasjon.konsistensavstemmingsdata.v1.Periode
import no.nav.virksomhet.tjenester.avstemming.informasjon.konsistensavstemmingsdata.v1.Totaldata
import java.math.BigDecimal
import java.time.LocalDate

internal class KonsistensavstemmingDataMapper(
    private val avstemming: Konsistensavstemming,
    private val detaljerPrMelding: Int = ANTALL_DETALJER_PER_AVSTEMMINGMELDING_OPPDRAG
) {

    fun opprettAvstemmingsmelding(): List<Konsistensavstemmingsdata> =
        listOf(startmelding()) + datameldinger() + listOf(sluttmelding())

    private fun startmelding() = Konsistensavstemmingsdata().apply {
        aksjonsdata = avstemmingsdata(KodeAksjon.START)
    }

    private fun sluttmelding() = Konsistensavstemmingsdata().apply {
        aksjonsdata = avstemmingsdata(KodeAksjon.AVSL)
    }

    private fun datameldinger(): List<Konsistensavstemmingsdata> {
        return avstemming.loependeUtbetalinger
            .chunked(detaljerPrMelding)
            .map {
                Konsistensavstemmingsdata().apply {
                    this.aksjonsdata = avstemmingsdata(KodeAksjon.DATA)
                    this.oppdragsdataListe.addAll(it.map { it.toOppdragdata() })
                }
            }.ifEmpty {
                listOf(
                    Konsistensavstemmingsdata().apply {
                        aksjonsdata = avstemmingsdata(KodeAksjon.DATA)
                    }
                )
            }.leggPaaTotaldata()
    }

    private fun List<Konsistensavstemmingsdata>.leggPaaTotaldata() =
        this.also {
            it.first().apply {
                totaldata = totaldata()
            }
        }

    private fun totaldata(): Totaldata =
        Totaldata().apply {
            totalAntall = avstemming.loependeUtbetalinger.count().toBigInteger()
            totalBelop = avstemming.loependeUtbetalinger
                .flatMap { it.utbetalingslinjer }
                .sumOf { it.beloep ?: BigDecimal.ZERO }
            fortegn = OppdragDefaults.TILLEGG.value()
        }

    private fun avstemmingsdata(kodeAksjon: KodeAksjon): Aksjonsdata =
        Aksjonsdata().apply {
            val fagomraade = "BARNEPE"
            aksjonsType = kodeAksjon.name
            kildeType = KildeType.AVLEV.name
            avstemmingType = KONSAVSTEMMING
            avleverendeKomponentKode = OppdragDefaults.AVLEVERENDE_KOMPONENTKODE
            mottakendeKomponentKode = OppdragDefaults.MOTTAKENDE_KOMPONENTKODE
            underkomponentKode = fagomraade
            avleverendeAvstemmingId = avstemming.id.value
            brukerId = fagomraade
            tidspunktAvstemmingTom = avstemming.opprettetTilOgMed.toNorskTid().format(tidsstempelMikroOppdrag)
        }
}

internal fun OppdragForKonsistensavstemming.toOppdragdata(): Oppdragsdata {
    return Oppdragsdata().apply {
        fagomradeKode = "BARNEPE"
        fagsystemId = sakId.value.toString()
        utbetalingsfrekvens = OppdragDefaults.UTBETALINGSFREKVENS
        oppdragGjelderId = fnr.value
        oppdragGjelderFom = LocalDate.EPOCH.format(tidsstempelDatoOppdrag)
        saksbehandlerId = OppdragDefaults.SAKSBEHANDLER_ID_SYSTEM_ETTERLATTEYTELSER
        oppdragsenhetListe.addAll(
            listOf(OppdragDefaults.OPPDRAGSENHET).map {
                Enhet().apply {
                    this.enhet = it.enhet
                    this.enhetType = it.typeEnhet
                    this.enhetFom = it.datoEnhetFom.toXMLFormat()
                }
            }
        )
        oppdragslinjeListe.addAll(
            utbetalingslinjer.map {
                Oppdragslinje().apply {
                    delytelseId = it.id.value.toString()
                    klassifikasjonKode = OppdragDefaults.KODEKOMPONENT.toString()
                    vedtakPeriode = Periode().apply {
                        fom = it.fraOgMed.format(tidsstempelDatoOppdrag)
                        tom = it.tilOgMed?.format(tidsstempelDatoOppdrag)
                    }
                    sats = it.beloep
                    satstypeKode = OppdragslinjeDefaults.UTBETALINGSFREKVENS
                    fradragTillegg = OppdragslinjeDefaults.FRADRAG_ELLER_TILLEGG.value()
                    brukKjoreplan = it.brukKjoereplan.toString()
                    utbetalesTilId = fnr.value
                    attestantListe.addAll(
                        it.attestanter.map {
                            Attestant().apply {
                                this.attestantId = it.value
                            }
                        }
                    )
                }
            }
        )
    }
}

enum class KodeAksjon {
    START, DATA, AVSL, HENT
}

enum class KildeType {
    AVLEV, MOTT
}

const val KONSAVSTEMMING = "KONS"