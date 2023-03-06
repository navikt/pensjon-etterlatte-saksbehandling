package no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.utbetaling.common.ANTALL_DETALJER_PER_AVSTEMMINGMELDING_OPPDRAG
import no.nav.etterlatte.utbetaling.common.OppdragDefaults
import no.nav.etterlatte.utbetaling.common.tidsstempelMikroOppdrag
import no.nav.etterlatte.utbetaling.common.tidsstempelTimeOppdrag
import no.nav.etterlatte.utbetaling.grensesnittavstemming.UUIDBase64
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetaling
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.AksjonType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Aksjonsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.AvstemmingType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.DetaljType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Detaljdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Fortegn
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Grunnlagsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.KildeType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Periodedata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Totaldata
import java.math.BigDecimal

class GrensesnittavstemmingDataMapper(
    private val utbetalinger: List<Utbetaling>,
    private val periodeFraOgMed: Tidspunkt,
    private val periodeTil: Tidspunkt,
    private val avstemmingId: UUIDBase64,
    private val detaljerPrMelding: Int = ANTALL_DETALJER_PER_AVSTEMMINGMELDING_OPPDRAG
) {
    private val avstemmingsperiode = periode(utbetalinger)

    fun opprettAvstemmingsmelding(): List<Avstemmingsdata> =
        startmelding() + datameldinger() + sluttmelding()

    private fun startmelding() = listOf(avstemmingsdata(AksjonType.START))

    private fun datameldinger(): List<Avstemmingsdata> =
        avstemmingsdataLister().ifEmpty { listOf(avstemmingsdata(AksjonType.DATA)) }.let {
            it.first().apply {
                total = totaldata()
                periode = periodedata()
                grunnlag = grunnlagsdata()
            }
            it
        }

    private fun sluttmelding() = listOf(avstemmingsdata(AksjonType.AVSL))

    private fun avstemmingsdata(aksjonstype: AksjonType) =
        Avstemmingsdata().apply {
            aksjon = Aksjonsdata().apply {
                val fagomraade = "BARNEPE"

                aksjonType = aksjonstype
                kildeType = KildeType.AVLEV
                avstemmingType = AvstemmingType.GRSN
                avleverendeKomponentKode = OppdragDefaults.AVLEVERENDE_KOMPONENTKODE
                mottakendeKomponentKode = OppdragDefaults.MOTTAKENDE_KOMPONENTKODE
                underkomponentKode = fagomraade
                nokkelFom =
                    avstemmingsperiode?.start?.toNorskTid()?.format(tidsstempelMikroOppdrag) ?: "0"
                nokkelTom =
                    avstemmingsperiode?.endInclusive?.toNorskTid()?.format(tidsstempelMikroOppdrag) ?: "0"
                avleverendeAvstemmingId = avstemmingId.value
                brukerId = fagomraade
            }
        }

    private fun avstemmingsdataLister(): List<Avstemmingsdata> {
        return detaljdata(utbetalinger).chunked(detaljerPrMelding).map {
            avstemmingsdata(AksjonType.DATA).apply {
                this.detalj.addAll(it)
            }
        }
    }

    private fun detaljdata(utbetaling: List<Utbetaling>): List<Detaljdata> =
        utbetaling.mapNotNull {
            val detaljType = toDetaljType(it.status())
            if (detaljType != null) {
                Detaljdata().apply {
                    this.detaljType = detaljType
                    offnr = it.stoenadsmottaker.value
                    avleverendeTransaksjonNokkel = it.sakId.value.toString()
                    tidspunkt = it.avstemmingsnoekkel.toNorskTid().format(tidsstempelMikroOppdrag)
                    if (detaljType in listOf(DetaljType.AVVI, DetaljType.VARS) && it.kvittering != null) {
                        meldingKode = it.kvittering.kode
                        alvorlighetsgrad = it.kvittering.alvorlighetsgrad
                        tekstMelding = it.kvittering.beskrivelse
                    }
                }
            } else {
                null
            }
        }

    private fun toDetaljType(utbetalingStatus: UtbetalingStatus): DetaljType? =
        when (utbetalingStatus) {
            UtbetalingStatus.SENDT -> DetaljType.MANG
            UtbetalingStatus.GODKJENT_MED_FEIL -> DetaljType.VARS
            UtbetalingStatus.AVVIST -> DetaljType.AVVI
            UtbetalingStatus.FEILET -> DetaljType.AVVI
            UtbetalingStatus.GODKJENT, UtbetalingStatus.MOTTATT -> null
        }

    private fun grunnlagsdata() = Grunnlagsdata().apply {
        val utbetalinger = utbetalinger.groupBy { it.status() }

        godkjentAntall = getAntall(utbetalinger[UtbetalingStatus.GODKJENT])
        godkjentBelop = getBelop(utbetalinger[UtbetalingStatus.GODKJENT])
        godkjentFortegn = getFortegn(godkjentBelop)

        varselAntall = getAntall(utbetalinger[UtbetalingStatus.GODKJENT_MED_FEIL])
        varselBelop = getBelop(utbetalinger[UtbetalingStatus.GODKJENT_MED_FEIL])
        varselFortegn = getFortegn(varselBelop)

        avvistAntall = listOf(
            getAntall(utbetalinger[UtbetalingStatus.FEILET]),
            getAntall(utbetalinger[UtbetalingStatus.AVVIST])
        ).sum()
        avvistBelop = listOf(
            getBelop(utbetalinger[UtbetalingStatus.FEILET]),
            getBelop(utbetalinger[UtbetalingStatus.AVVIST])
        ).reduce(BigDecimal::add)
        avvistFortegn = getFortegn(avvistBelop)

        manglerAntall = getAntall(utbetalinger[UtbetalingStatus.SENDT])
        manglerBelop = getBelop(utbetalinger[UtbetalingStatus.SENDT])
        manglerFortegn = getFortegn(manglerBelop)
    }

    private fun getAntall(utbetalinger: List<Utbetaling>?) = utbetalinger?.count() ?: 0

    private fun getBelop(utbetalinger: List<Utbetaling>?) =
        utbetalinger?.sumOf {
            it.utbetalingslinjer
                .map { utbetalingslinje -> utbetalingslinje.beloep ?: BigDecimal.ZERO }
                .reduce(BigDecimal::add)
        } ?: BigDecimal.ZERO

    private fun getFortegn(belop: BigDecimal): Fortegn {
        return if (belop >= BigDecimal.ZERO) Fortegn.T else Fortegn.F
    }

    private fun totaldata() =
        Totaldata().apply {
            totalAntall = getAntall(utbetalinger)
            totalBelop = getBelop(utbetalinger)
            fortegn = getFortegn(totalBelop)
        }

    private fun periodedata() =
        Periodedata().apply {
            datoAvstemtFom = periodeFraOgMed.toNorskTid().format(tidsstempelTimeOppdrag)
            datoAvstemtTom = periodeTil.toNorskTid().minusHours(1).format(tidsstempelTimeOppdrag)
        }

    private fun periode(liste: List<Utbetaling>): ClosedRange<Tidspunkt>? {
        return if (liste.isEmpty()) {
            null
        } else {
            object : ClosedRange<Tidspunkt> {
                override val start = liste.minOf { it.avstemmingsnoekkel }
                override val endInclusive = liste.maxOf { it.avstemmingsnoekkel }
            }
        }
    }
}