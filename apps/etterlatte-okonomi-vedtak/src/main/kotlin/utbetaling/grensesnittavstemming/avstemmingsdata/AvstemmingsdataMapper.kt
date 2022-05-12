package no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata

import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetaling
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.nav.etterlatte.utbetaling.common.Tidspunkt
import no.nav.etterlatte.utbetaling.common.toNorskTid
import no.nav.etterlatte.utbetaling.grensesnittavstemming.UUIDBase64
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
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.math.BigDecimal

class AvstemmingsdataMapper(
    private val utbetalinger: List<Utbetaling>,
    private val periodeFraOgMed: Tidspunkt,
    private val periodeTil: Tidspunkt,
    private val avstemmingId: UUIDBase64,
    private val detaljerPrMelding: Int = ANTALL_DETALJER_PER_AVSTEMMINGMELDING
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
                avleverendeKomponentKode = "ETTERLAT"
                mottakendeKomponentKode = "OS"
                underkomponentKode = fagomraade
                nokkelFom =
                    avstemmingsperiode?.start?.let {
                        Tidspunkt(it).toNorskTid()
                    }?.format(tidsstempelMikro) ?: "0"
                nokkelTom =
                    avstemmingsperiode?.endInclusive?.let {
                        Tidspunkt(it).toNorskTid()
                    }?.format(tidsstempelMikro) ?: "0"
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
            val detaljType = toDetaljType(it.status)
            if (detaljType != null) {
                Detaljdata().apply {
                    this.detaljType = detaljType
                    offnr = it.foedselsnummer.value
                    avleverendeTransaksjonNokkel = it.sakId.value
                    tidspunkt = it.avstemmingsnoekkel.toNorskTid().format(tidsstempelTime)
                    if (detaljType in listOf(DetaljType.AVVI, DetaljType.VARS) && it.kvittering != null) {
                        meldingKode = it.kvitteringMeldingKode
                        alvorlighetsgrad = it.kvitteringFeilkode
                        tekstMelding = it.kvitteringBeskrivelse
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
            UtbetalingStatus.GODKJENT -> null
        }

    private fun grunnlagsdata() = Grunnlagsdata().apply {
        val utbetalinger = utbetalinger.groupBy { it.status }

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
            it.oppdrag.oppdrag110.oppdragsLinje150 // TODO er dette riktig sted å hente dette?
                .map { oppdragsLinje -> oppdragsLinje.sats }
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
            datoAvstemtFom = periodeFraOgMed.toNorskTid().format(tidsstempelTime)
            datoAvstemtTom = periodeTil.toNorskTid().minusHours(1).format(tidsstempelTime)
        }

    private fun periode(liste: List<Utbetaling>): ClosedRange<Instant>? {
        return if (liste.isEmpty()) null else {
            object : ClosedRange<Instant> {
                override val start = liste.minOf { it.avstemmingsnoekkel.instant }
                override val endInclusive = liste.maxOf { it.avstemmingsnoekkel.instant }
            }
        }
    }

    companion object {
        private const val ANTALL_DETALJER_PER_AVSTEMMINGMELDING = 70
        private val tidsstempelTime = DateTimeFormatter.ofPattern("yyyyMMddHH")
        private val tidsstempelMikro = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")
    }
}