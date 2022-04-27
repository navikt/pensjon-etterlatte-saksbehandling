package no.nav.etterlatte.avstemming

import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.AksjonType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Aksjonsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.AvstemmingType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Detaljdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Fortegn
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Grunnlagsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.KildeType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Periodedata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Totaldata
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class AvstemmingsdataMapper(id: UUID) {

    private val avleverendeAvstemmingsId = encodeUUIDBase64(id)
    private val antOverforteMeldinger = 1000 // TODO: hente det totale antallet oppdrag som er hentet
    private val totaltOverfortBelop = 0 // TODO: denne er frivillig. Kan utvides med denne
    private val grunnlagsdata: Grunnlagsdata = Grunnlagsdata() // TODO: metode for å hente grunnlagsdata
    private val detaljdata: List<Detaljdata> = listOf(Detaljdata()) // TODO: metode for å hente ut detaljdata
    // TODO: // trans-nokkel-avlev, skal dette være vedtak.sakId ?

    private companion object {
        private const val DETALJER_PER_AVSTEMMINGMELDING = 70
        private val tidsstempel = DateTimeFormatter.ofPattern("yyyyMMddHH")
    }

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
                //avleverendeAvstemmingId = // TODO: lage en unik ID for en avstemming
                brukerId = "BARNEPE" // TODO: finne ut hva som skal settes her
            }
        }

    fun avstemmingsdataData(avstemmingsdata: Avstemmingsdata) =
        avstemmingsdata.apply {
            total = Totaldata().apply {
                totalAntall = antOverforteMeldinger
                totalBelop = BigDecimal(totaltOverfortBelop)
                fortegn = Fortegn.T
            }

            grunnlag = grunnlagsdata

            detalj.addAll(detaljdata)

            periode = Periodedata().apply {
                datoAvstemtFom = LocalDateTime.MIN.format(tidsstempel) // TODO: finne timestamp for første record
                datoAvstemtTom = LocalDateTime.now().format(tidsstempel) // TODO: finne timestamp for siste record
            }


        }

    fun fullestendigAvstemmingsmelding() = listOf(
        avstemmingsdata(AksjonType.START),
        avstemmingsdataData(avstemmingsdata((AksjonType.DATA))),
        avstemmingsdata(AksjonType.AVSL)
    )


    private fun encodeUUIDBase64(uuid: UUID): String {
        val bb = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return Base64.getUrlEncoder().encodeToString(bb.array()).substring(0, 22)
    }


}