package no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata

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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AvstemmingsdataMapper(
    private val utbetalinger: List<Utbetaling>,
    private val fraOgMed: LocalDateTime,
    private val til: LocalDateTime,
    private val avstemmingId: String,
    private val detaljerPrMelding: Int = ANTALL_DETALJER_PER_AVSTEMMINGMELDING
) {
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
                val periode = periode(utbetalinger)
                val fagomraade = "BARNEPE"

                aksjonType = aksjonstype
                kildeType = KildeType.AVLEV
                avstemmingType = AvstemmingType.GRSN
                avleverendeKomponentKode = "ETTERLAT"
                mottakendeKomponentKode = "OS"
                underkomponentKode = fagomraade
                nokkelFom = periode?.start?.format(tidsstempelMikro) ?: "0"
                nokkelTom = periode?.endInclusive?.format(tidsstempelMikro) ?: "0"
                avleverendeAvstemmingId = avstemmingId
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
                    offnr = it.foedselsnummer
                    avleverendeTransaksjonNokkel = it.sakId
                    tidspunkt = it.avstemmingsnoekkel.format(tidsstempelTime)
                    if (detaljType in listOf(DetaljType.AVVI, DetaljType.VARS) && it.kvitteringOppdrag != null) {
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
            it.utgaaendeOppdrag.oppdrag110.oppdragsLinje150 // TODO er dette riktig sted å hente dette?
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
            datoAvstemtFom = fraOgMed.format(tidsstempelTime)
            datoAvstemtTom = til.minusHours(1).format(tidsstempelTime)
        }

    private fun periode(liste: List<Utbetaling>): ClosedRange<LocalDateTime>? {
        return if (liste.isEmpty()) null else {
            object : ClosedRange<LocalDateTime> {
                override val start = liste.minOf { it.avstemmingsnoekkel }
                override val endInclusive = liste.maxOf { it.avstemmingsnoekkel }
            }
        }
    }

    companion object {
        private const val ANTALL_DETALJER_PER_AVSTEMMINGMELDING = 70
        private val tidsstempelTime = DateTimeFormatter.ofPattern("yyyyMMddHH")
        private val tidsstempelMikro = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")
    }
}