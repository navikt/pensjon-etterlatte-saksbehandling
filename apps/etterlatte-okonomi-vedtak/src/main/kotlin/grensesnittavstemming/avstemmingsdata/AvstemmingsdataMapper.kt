package no.nav.etterlatte.grensesnittavstemming.avstemmingsdata

import no.nav.etterlatte.iverksetting.utbetaling.Utbetaling
import no.nav.etterlatte.iverksetting.utbetaling.UtbetalingStatus
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.AksjonType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Aksjonsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.AvstemmingType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.DetaljType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Detaljdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Grunnlagsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.KildeType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Periodedata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Totaldata
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

                aksjonType = aksjonstype
                kildeType = KildeType.AVLEV
                avstemmingType = AvstemmingType.GRSN
                avleverendeKomponentKode = "ETTERLAT"
                mottakendeKomponentKode = "OS"
                underkomponentKode = "BARNEPE"
                nokkelFom = periode?.start?.format(tidsstempelMikro) ?: "0"
                nokkelTom = periode?.endInclusive?.format(tidsstempelMikro) ?: "0"
                avleverendeAvstemmingId = avstemmingId
                brukerId = "ETTERLAT" // TODO: systembruker - definere selv
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
                    if (detaljType in listOf(DetaljType.AVVI, DetaljType.VARS) && it.oppdragKvittering != null) {
                        meldingKode = it.meldingKodeOppdrag
                        alvorlighetsgrad = it.feilkodeOppdrag
                        tekstMelding = it.beskrivelseOppdrag
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
        val utbetalingEtterStatus = utbetalinger.groupBy { it.status }
        val antFeilet = utbetalingEtterStatus[UtbetalingStatus.FEILET]?.count() ?: 0
        val antAvvist = utbetalingEtterStatus[UtbetalingStatus.AVVIST]?.count() ?: 0
        godkjentAntall = utbetalingEtterStatus[UtbetalingStatus.GODKJENT]?.count() ?: 0
        varselAntall = utbetalingEtterStatus[UtbetalingStatus.GODKJENT_MED_FEIL]?.count() ?: 0
        avvistAntall = antFeilet + antAvvist
        manglerAntall = utbetalingEtterStatus[UtbetalingStatus.SENDT]?.count() ?: 0
    }

    private fun totaldata() =
        Totaldata().apply {
            totalAntall = utbetalinger.size
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