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

class AvstemmingsdataMapper(
    private val utbetalingsoppdrag: List<Utbetalingsoppdrag>,
    private val id: UUID,
    private val detaljerPrMelding: Int = ANTALL_DETALJER_PER_AVSTEMMINGMELDING
) {
    private val avleverendeAvstemmingsId = encodeUUIDBase64(id)

    fun opprettAvstemmingsmelding(): List<Avstemmingsdata> =
        if (utbetalingsoppdrag.isEmpty()) emptyList()
        else startmelding() + datameldinger() + sluttmelding()

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

    private fun avstemmingsdataLister() : List<Avstemmingsdata> {
        return detaljdata(utbetalingsoppdrag).chunked(detaljerPrMelding).map {
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
                    // TODO: // trans-nokkel-avlev, skal dette være vedtak.sakId ?
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
    fun grunnlagsdata() = Grunnlagsdata().apply {
        val oppdragEtterStatus = utbetalingsoppdrag.groupBy { it.status }

        godkjentAntall = oppdragEtterStatus[UtbetalingsoppdragStatus.GODKJENT]?.count() ?: 0
        varselAntall = oppdragEtterStatus[UtbetalingsoppdragStatus.GODKJENT_MED_FEIL]?.count() ?: 0
        avvistAntall = oppdragEtterStatus[UtbetalingsoppdragStatus.AVVIST]?.count() ?: 0
        val antFeilet = oppdragEtterStatus[UtbetalingsoppdragStatus.FEILET]?.count() ?: 0
        val antSendtUtenKvittering = oppdragEtterStatus[UtbetalingsoppdragStatus.SENDT]?.count() ?: 0
        manglerAntall = antFeilet + antSendtUtenKvittering
    }

    private fun totaldata() =
        Totaldata().apply {
            totalAntall = utbetalingsoppdrag.size
        }

    private fun periodedata() =
        Periodedata().apply {
            val periode = periode(utbetalingsoppdrag)
            datoAvstemtFom = periode.start
            datoAvstemtTom = periode.endInclusive
        }

    // TODO denne bør egentlig ikke være public
    fun periode(liste: List<Utbetalingsoppdrag>): ClosedRange<String> {
        check(liste.isNotEmpty())
        return object : ClosedRange<String> {
            override val start = liste.minOf { it.avstemmingsnoekkel }.format(tidsstempel)
            override val endInclusive = liste.maxOf { it.avstemmingsnoekkel }.format(tidsstempel)
        }
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
}