package no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata

import no.nav.etterlatte.utbetaling.common.Tidspunkt
import no.nav.etterlatte.utbetaling.common.toNorskTid
import no.nav.etterlatte.utbetaling.common.toTidspunkt
import no.nav.etterlatte.utbetaling.grensesnittavstemming.UUIDBase64
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.nav.etterlatte.utbetaling.utbetaling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

internal class AvstemmingsdataJaxbTest {

    private val formatterMicro = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")
    private val formatterTime = DateTimeFormatter.ofPattern("yyyyMMddHH")

    @Test
    fun `skal konvertere avstemmingsdata til gyldig xml`() {
        val now = Instant.now()
        val fraOgMed = now.minus(1, ChronoUnit.DAYS).toTidspunkt()
        val til = now.toTidspunkt()

        val uuid = UUIDBase64()
        val utbetaling = listOf(
            utbetaling(id = 1, status = UtbetalingStatus.FEILET, avstemmingsnoekkel = til, opprettet = til)
        )

        val avstemmingsdataMapper = AvstemmingsdataMapper(utbetaling, fraOgMed, til, uuid)
        val (startMelding, dataMelding, sluttMelding) = avstemmingsdataMapper.opprettAvstemmingsmelding()

        val startMeldingXml = AvstemmingsdataJaxb.toXml(startMelding)
        val dataMeldingXml = AvstemmingsdataJaxb.toXml(dataMelding)
        val sluttMeldingXml = AvstemmingsdataJaxb.toXml(sluttMelding)

        assertEquals(gyldigStartMelding(uuid, utbetaling.first().avstemmingsnoekkel), startMeldingXml)
        assertEquals(gyldigDataMelding(uuid, fraOgMed, til, utbetaling.first().avstemmingsnoekkel), dataMeldingXml)
        assertEquals(gyldigSluttMelding(uuid, utbetaling.first().avstemmingsnoekkel), sluttMeldingXml)
    }

    private fun gyldigStartMelding(uuid: UUIDBase64, avstemmingsnoekkel: Tidspunkt) =
        gyldigMelding("START", uuid, avstemmingsnoekkel)

    private fun gyldigSluttMelding(uuid: UUIDBase64, avstemmingsnoekkel: Tidspunkt) =
        gyldigMelding("AVSL", uuid, avstemmingsnoekkel)

    private fun gyldigMelding(aksjonsType: String, uuid: UUIDBase64, avstemmingsnoekkel: Tidspunkt) = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Avstemmingsdata>
            <aksjon>
                <aksjonType>$aksjonsType</aksjonType>
                <kildeType>AVLEV</kildeType>
                <avstemmingType>GRSN</avstemmingType>
                <avleverendeKomponentKode>ETTERLAT</avleverendeKomponentKode>
                <mottakendeKomponentKode>OS</mottakendeKomponentKode>
                <underkomponentKode>BARNEPE</underkomponentKode>
                <nokkelFom>${formatterMicro.format(avstemmingsnoekkel.toNorskTid())}</nokkelFom>
                <nokkelTom>${formatterMicro.format(avstemmingsnoekkel.toNorskTid())}</nokkelTom>
                <avleverendeAvstemmingId>${uuid.value}</avleverendeAvstemmingId>
                <brukerId>BARNEPE</brukerId>
            </aksjon>
        </Avstemmingsdata>
        
    """.trimIndent()

    private fun gyldigDataMelding(uuid: UUIDBase64, fra: Tidspunkt, til: Tidspunkt, avstemmingsnoekkel: Tidspunkt) = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Avstemmingsdata>
            <aksjon>
                <aksjonType>DATA</aksjonType>
                <kildeType>AVLEV</kildeType>
                <avstemmingType>GRSN</avstemmingType>
                <avleverendeKomponentKode>ETTERLAT</avleverendeKomponentKode>
                <mottakendeKomponentKode>OS</mottakendeKomponentKode>
                <underkomponentKode>BARNEPE</underkomponentKode>
                <nokkelFom>${formatterMicro.format(avstemmingsnoekkel.toNorskTid())}</nokkelFom>
                <nokkelTom>${formatterMicro.format(avstemmingsnoekkel.toNorskTid())}</nokkelTom>
                <avleverendeAvstemmingId>${uuid.value}</avleverendeAvstemmingId>
                <brukerId>BARNEPE</brukerId>
            </aksjon>
            <total>
                <totalAntall>1</totalAntall>
                <totalBelop>10000</totalBelop>
                <fortegn>T</fortegn>
            </total>
            <periode>
                <datoAvstemtFom>${formatterTime.format(fra.toNorskTid())}</datoAvstemtFom>
                <datoAvstemtTom>${formatterTime.format(til.toNorskTid().minusHours(1))}</datoAvstemtTom>
            </periode>
            <grunnlag>
                <godkjentAntall>0</godkjentAntall>
                <godkjentBelop>0</godkjentBelop>
                <godkjentFortegn>T</godkjentFortegn>
                <varselAntall>0</varselAntall>
                <varselBelop>0</varselBelop>
                <varselFortegn>T</varselFortegn>
                <avvistAntall>1</avvistAntall>
                <avvistBelop>10000</avvistBelop>
                <avvistFortegn>T</avvistFortegn>
                <manglerAntall>0</manglerAntall>
                <manglerBelop>0</manglerBelop>
                <manglerFortegn>T</manglerFortegn>
            </grunnlag>
            <detalj>
                <detaljType>AVVI</detaljType>
                <offnr>12345678903</offnr>
                <avleverendeTransaksjonNokkel>1</avleverendeTransaksjonNokkel>
                <meldingKode>08</meldingKode>
                <alvorlighetsgrad>hva skal stå her?</alvorlighetsgrad>
                <tekstMelding>En beskrivelse</tekstMelding>
                <tidspunkt>${formatterMicro.format(avstemmingsnoekkel.toNorskTid())}</tidspunkt>
            </detalj>
        </Avstemmingsdata>
        
    """.trimIndent()
}