package no.nav.etterlatte.avstemming

import no.nav.etterlatte.domain.Utbetalingsoppdrag
import no.nav.etterlatte.domain.UtbetalingsoppdragStatus
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
import java.nio.ByteBuffer
import java.time.format.DateTimeFormatter
import java.util.*

class AvstemmingsdataMapper(val utbetalingsoppdrag: List<Utbetalingsoppdrag>, id: UUID) {

    private val avleverendeAvstemmingsId = encodeUUIDBase64(id)
    private val antOverforteMeldinger = utbetalingsoppdrag.size
    private val avstemmingPeriode = periode(utbetalingsoppdrag)
    // TODO: // trans-nokkel-avlev, skal dette være vedtak.sakId ?

    fun avstemmingsmelding(): List<Avstemmingsdata> =
        if (utbetalingsoppdrag.isEmpty()) emptyList()
        else listOf(startmelding()) + datameldinger() + listOf(sluttmelding())

    private fun startmelding() = avstemmingsdata(AksjonType.START)

    private fun datameldinger(): List<Avstemmingsdata> =
        avstemmingsdataLister().ifEmpty { listOf(avstemmingsdata(AksjonType.DATA)) }.let {
            it.first().apply {
                this.total = totaldata()
                this.periode = Periodedata().apply {
                    datoAvstemtFom = avstemmingPeriode.start
                    datoAvstemtTom = avstemmingPeriode.endInclusive
                }
                this.grunnlag = grunnlagsdata(utbetalingsoppdrag)
            }
            it
        }

    private fun sluttmelding() = avstemmingsdata(AksjonType.AVSL)

    private fun avstemmingsdata(aksjonstype: AksjonType) =
        Avstemmingsdata().apply {
            // 110
            aksjon = Aksjonsdata().apply {
                aksjonType = aksjonstype
                kildeType = KildeType.AVLEV
                avstemmingType = AvstemmingType.GRSN
                avleverendeKomponentKode = "BARNEPE" // TODO: korrekt?
                mottakendeKomponentKode = "OS"
                underkomponentKode = "BARNEPE" // TODO: korrekt?
                //nokkelFom =  // TODO: logikk for å finne laveste avstemmingsnøkkel
                // nokkelTom // TODO: logikk for å finne høyeste avstemmingsnøkkel
                avleverendeAvstemmingId = avleverendeAvstemmingsId
                brukerId = "BARNEPE" // TODO: finne ut hva som skal settes her
            }
        }

    private fun avstemmingsdataLister(): List<Avstemmingsdata> {
        return detaljdata(utbetalingsoppdrag).chunked(ANTALL_DETALJER_PER_AVSTEMMINGMELDING).map {
            avstemmingsdata(AksjonType.DATA).apply {
                this.detalj.addAll(it)
            }
        }
    }

    private fun detaljdata(utbetalingsoppdrag: List<Utbetalingsoppdrag>): List<Detaljdata> =
        utbetalingsoppdrag.mapNotNull {
            val detaljType = toDetaljType(it.status)
            if (detaljType != null) {
                Detaljdata().apply {
                    this.detaljType = detaljType
                    offnr = it.foedselsnummer
                    avleverendeTransaksjonNokkel = it.avstemmingsnoekkel.format(tidsstempel)
                    tidspunkt = it.avstemmingsnoekkel.format(tidsstempel)
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

    private fun toDetaljType(utbetalingsoppdragStatus: UtbetalingsoppdragStatus): DetaljType? =
        when (utbetalingsoppdragStatus) {
            UtbetalingsoppdragStatus.SENDT -> DetaljType.MANG
            UtbetalingsoppdragStatus.GODKJENT_MED_FEIL -> DetaljType.VARS
            UtbetalingsoppdragStatus.AVVIST -> DetaljType.AVVI
            UtbetalingsoppdragStatus.FEILET -> DetaljType.AVVI
            UtbetalingsoppdragStatus.GODKJENT -> null
        }

    // TODO denne bør egentlig ikke være public
    fun grunnlagsdata(liste: List<Utbetalingsoppdrag>) = Grunnlagsdata().apply {
        val oppdragEtterStatus = liste.groupBy { it.status }

        godkjentAntall = oppdragEtterStatus[UtbetalingsoppdragStatus.GODKJENT]?.count() ?: 0
        varselAntall = oppdragEtterStatus[UtbetalingsoppdragStatus.GODKJENT_MED_FEIL]?.count() ?: 0
        avvistAntall = oppdragEtterStatus[UtbetalingsoppdragStatus.AVVIST]?.count() ?: 0
        val antFeilet = oppdragEtterStatus[UtbetalingsoppdragStatus.FEILET]?.count() ?: 0
        val antSendtUtenKvittering = oppdragEtterStatus[UtbetalingsoppdragStatus.SENDT]?.count() ?: 0
        manglerAntall = antFeilet + antSendtUtenKvittering
    }

    private fun totaldata() =
        Totaldata().apply {
            totalAntall = antOverforteMeldinger
        }

    private fun encodeUUIDBase64(uuid: UUID): String {
        val bb = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return Base64.getUrlEncoder().encodeToString(bb.array()).substring(0, 22)
    }

    companion object {
        private const val ANTALL_DETALJER_PER_AVSTEMMINGMELDING = 70
        private val tidsstempel = DateTimeFormatter.ofPattern("yyyyMMddHH")
    }

    fun periode(liste: List<Utbetalingsoppdrag>): ClosedRange<String> {
        check(liste.isNotEmpty())
        return object : ClosedRange<String> {
            override val start = liste.minOf { it.avstemmingsnoekkel }.format(tidsstempel)
            override val endInclusive = liste.maxOf { it.avstemmingsnoekkel }.format(tidsstempel)
        }
    }

}