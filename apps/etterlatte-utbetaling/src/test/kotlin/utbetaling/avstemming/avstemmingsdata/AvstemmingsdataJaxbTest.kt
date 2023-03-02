package no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.utbetaling.avstemming.avstemmingsdata.KonsistensavstemmingDataMapper
import no.nav.etterlatte.utbetaling.common.tidsstempelMikroOppdrag
import no.nav.etterlatte.utbetaling.common.tidsstempelTimeOppdrag
import no.nav.etterlatte.utbetaling.grensesnittavstemming.UUIDBase64
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.nav.etterlatte.utbetaling.kvittering
import no.nav.etterlatte.utbetaling.mockKonsistensavstemming
import no.nav.etterlatte.utbetaling.oppdrag
import no.nav.etterlatte.utbetaling.oppdragForKonsistensavstemming
import no.nav.etterlatte.utbetaling.oppdragMedFeiletKvittering
import no.nav.etterlatte.utbetaling.oppdragslinjeForKonsistensavstemming
import no.nav.etterlatte.utbetaling.utbetaling
import no.nav.etterlatte.utbetaling.utbetalingshendelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.*

internal class AvstemmingsdataJaxbTest {

    @Test
    fun `skal konvertere konsistensavstemmingsdata til gyldig xml`() {
        val idag = LocalDate.now()
        val konsistensavstemmingId = UUIDBase64()
        val tidspunktAvstemmingTom = idag.minusDays(1).atTime(LocalTime.MAX).toNorskTidspunkt()
        val oppdragslinjer = listOf(oppdragslinjeForKonsistensavstemming(fraOgMed = LocalDate.of(2022, 10, 7)))
        val oppdrag = oppdragForKonsistensavstemming(oppdragslinjeForKonsistensavstemming = oppdragslinjer)
        val konsistensavstemming = mockKonsistensavstemming(
            loependeUtbetalinger = listOf(oppdrag),
            dag = idag,
            id = konsistensavstemmingId,
            opprettTilOgMed = tidspunktAvstemmingTom
        )
        val konsistensavstemmingDataMapper = KonsistensavstemmingDataMapper(konsistensavstemming)
        val (startMelding, dataMelding, sluttMelding) = konsistensavstemmingDataMapper.opprettAvstemmingsmelding()

        val startMeldingXml = KonsistensavstemmingsdataJaxb.toXml(startMelding)
        val dataMeldingXml = KonsistensavstemmingsdataJaxb.toXml(dataMelding)
        val sluttMeldingXml = KonsistensavstemmingsdataJaxb.toXml(sluttMelding)

        assertEquals(
            gyldigKonsistensavstemmingStartMelding(
                tidspunktAvstemmingFom = tidspunktAvstemmingTom,
                uuid = konsistensavstemmingId
            ),
            startMeldingXml
        )

        assertEquals(
            gyldigKonsistensavstemmingDataMelding(
                tidspunktAvstemmingTom = tidspunktAvstemmingTom,
                uuid = konsistensavstemmingId
            ),
            dataMeldingXml
        )

        assertEquals(
            gyldigKonsistensavstemmingAvslMelding(
                tidspunktAvstemmingFom = tidspunktAvstemmingTom,
                uuid = konsistensavstemmingId
            ),
            sluttMeldingXml
        )
    }

    private fun gyldigKonsistensavstemmingStartMelding(tidspunktAvstemmingFom: Tidspunkt, uuid: UUIDBase64) =
        gyldigKonsistensavstemmingMelding(
            aksjonstype = "START",
            tidspunktAvstemmingTom = tidspunktAvstemmingFom,
            avleverendeAvstemmingId = uuid
        )

    private fun gyldigKonsistensavstemmingAvslMelding(tidspunktAvstemmingFom: Tidspunkt, uuid: UUIDBase64) =
        gyldigKonsistensavstemmingMelding(
            aksjonstype = "AVSL",
            tidspunktAvstemmingTom = tidspunktAvstemmingFom,
            avleverendeAvstemmingId = uuid
        )

    private fun gyldigKonsistensavstemmingMelding(
        aksjonstype: String,
        tidspunktAvstemmingTom: Tidspunkt,
        avleverendeAvstemmingId: UUIDBase64
    ) = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Konsistensavstemmingsdata>
            <aksjonsdata>
                <aksjonsType>$aksjonstype</aksjonsType>
                <kildeType>AVLEV</kildeType>
                <avstemmingType>KONS</avstemmingType>
                <avleverendeKomponentKode>ETTERLAT</avleverendeKomponentKode>
                <mottakendeKomponentKode>OS</mottakendeKomponentKode>
                <underkomponentKode>BARNEPE</underkomponentKode>
                <tidspunktAvstemmingTom>${tidsstempelMikroOppdrag.format(tidspunktAvstemmingTom.toNorskTid())}</tidspunktAvstemmingTom>
                <avleverendeAvstemmingId>${avleverendeAvstemmingId.value}</avleverendeAvstemmingId>
                <brukerId>BARNEPE</brukerId>
            </aksjonsdata>
        </Konsistensavstemmingsdata>
        
    """.trimIndent()

    private fun gyldigKonsistensavstemmingDataMelding(
        uuid: UUIDBase64,
        tidspunktAvstemmingTom: Tidspunkt

    ) =
        """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Konsistensavstemmingsdata>
                <aksjonsdata>
                    <aksjonsType>DATA</aksjonsType>
                    <kildeType>AVLEV</kildeType>
                    <avstemmingType>KONS</avstemmingType>
                    <avleverendeKomponentKode>ETTERLAT</avleverendeKomponentKode>
                    <mottakendeKomponentKode>OS</mottakendeKomponentKode>
                    <underkomponentKode>BARNEPE</underkomponentKode>
                    <tidspunktAvstemmingTom>${tidsstempelMikroOppdrag.format(tidspunktAvstemmingTom.toNorskTid())}</tidspunktAvstemmingTom>
                    <avleverendeAvstemmingId>${uuid.value}</avleverendeAvstemmingId>
                    <brukerId>BARNEPE</brukerId>
                </aksjonsdata>
                <oppdragsdataListe>
                    <fagomradeKode>BARNEPE</fagomradeKode>
                    <fagsystemId>1</fagsystemId>
                    <utbetalingsfrekvens>MND</utbetalingsfrekvens>
                    <oppdragGjelderId>123456</oppdragGjelderId>
                    <oppdragGjelderFom>1970-01-01</oppdragGjelderFom>
                    <saksbehandlerId>EY</saksbehandlerId>
                    <oppdragsenhetListe>
                        <enhetType>BOS</enhetType>
                        <enhet>4819</enhet>
                        <enhetFom>1900-01-01T00:00:00.000</enhetFom>
                    </oppdragsenhetListe>
                    <oppdragslinjeListe>
                        <delytelseId>1</delytelseId>
                        <klassifikasjonKode>BARNEPENSJON-OPTP</klassifikasjonKode>
                        <vedtakPeriode>
                            <fom>2022-10-07</fom>
                        </vedtakPeriode>
                        <sats>10000</sats>
                        <satstypeKode>MND</satstypeKode>
                        <fradragTillegg>T</fradragTillegg>
                        <brukKjoreplan>J</brukKjoreplan>
                        <utbetalesTilId>123456</utbetalesTilId>
                        <attestantListe>
                            <attestantId>attestant</attestantId>
                        </attestantListe>
                    </oppdragslinjeListe>
                </oppdragsdataListe>
                <totaldata>
                    <totalAntall>1</totalAntall>
                    <totalBelop>10000</totalBelop>
                    <fortegn>T</fortegn>
                </totaldata>
            </Konsistensavstemmingsdata>

        """.trimIndent()

    @Test
    fun `skal konvertere grensesnittavstemmingsdata til gyldig xml`() {
        val now = Instant.now()
        val fraOgMed = now.minus(1, ChronoUnit.DAYS).toTidspunkt()
        val til = now.toTidspunkt()

        val uuid = UUIDBase64()
        val utbetalingId = UUID.randomUUID()
        val utbetaling = listOf(
            utbetaling(
                id = utbetalingId,
                avstemmingsnoekkel = til,
                opprettet = til,
                utbetalingshendelser = listOf(
                    utbetalingshendelse(
                        utbetalingId = utbetalingId,
                        status = UtbetalingStatus.FEILET
                    )
                )
            ).let { it.copy(oppdrag = oppdrag(it), kvittering = kvittering(oppdragMedFeiletKvittering())) }
        )

        val grensesnittavstemmingDataMapper = GrensesnittavstemmingDataMapper(utbetaling, fraOgMed, til, uuid)
        val (startMelding, dataMelding, sluttMelding) = grensesnittavstemmingDataMapper.opprettAvstemmingsmelding()

        val startMeldingXml = GrensesnittavstemmingsdataJaxb.toXml(startMelding)
        val dataMeldingXml = GrensesnittavstemmingsdataJaxb.toXml(dataMelding)
        val sluttMeldingXml = GrensesnittavstemmingsdataJaxb.toXml(sluttMelding)

        assertEquals(
            gyldigGrensesnittavstemmingStartMelding(uuid, utbetaling.first().avstemmingsnoekkel),
            startMeldingXml
        )
        assertEquals(
            gyldigGrensesnittavstemmingDataMelding(uuid, fraOgMed, til, utbetaling.first().avstemmingsnoekkel),
            dataMeldingXml
        )
        assertEquals(
            gyldigGrensesnittavstemmingSluttMelding(uuid, utbetaling.first().avstemmingsnoekkel),
            sluttMeldingXml
        )
    }

    private fun gyldigGrensesnittavstemmingStartMelding(uuid: UUIDBase64, avstemmingsnoekkel: Tidspunkt) =
        gyldigGrensesnittavstemmingMelding("START", uuid, avstemmingsnoekkel)

    private fun gyldigGrensesnittavstemmingSluttMelding(uuid: UUIDBase64, avstemmingsnoekkel: Tidspunkt) =
        gyldigGrensesnittavstemmingMelding("AVSL", uuid, avstemmingsnoekkel)

    private fun gyldigGrensesnittavstemmingMelding(
        aksjonsType: String,
        uuid: UUIDBase64,
        avstemmingsnoekkel: Tidspunkt
    ) = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Avstemmingsdata>
            <aksjon>
                <aksjonType>$aksjonsType</aksjonType>
                <kildeType>AVLEV</kildeType>
                <avstemmingType>GRSN</avstemmingType>
                <avleverendeKomponentKode>ETTERLAT</avleverendeKomponentKode>
                <mottakendeKomponentKode>OS</mottakendeKomponentKode>
                <underkomponentKode>BARNEPE</underkomponentKode>
                <nokkelFom>${tidsstempelMikroOppdrag.format(avstemmingsnoekkel.toNorskTid())}</nokkelFom>
                <nokkelTom>${tidsstempelMikroOppdrag.format(avstemmingsnoekkel.toNorskTid())}</nokkelTom>
                <avleverendeAvstemmingId>${uuid.value}</avleverendeAvstemmingId>
                <brukerId>BARNEPE</brukerId>
            </aksjon>
        </Avstemmingsdata>
        
    """.trimIndent()

    private fun gyldigGrensesnittavstemmingDataMelding(
        uuid: UUIDBase64,
        fra: Tidspunkt,
        til: Tidspunkt,
        avstemmingsnoekkel: Tidspunkt
    ) = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Avstemmingsdata>
            <aksjon>
                <aksjonType>DATA</aksjonType>
                <kildeType>AVLEV</kildeType>
                <avstemmingType>GRSN</avstemmingType>
                <avleverendeKomponentKode>ETTERLAT</avleverendeKomponentKode>
                <mottakendeKomponentKode>OS</mottakendeKomponentKode>
                <underkomponentKode>BARNEPE</underkomponentKode>
                <nokkelFom>${tidsstempelMikroOppdrag.format(avstemmingsnoekkel.toNorskTid())}</nokkelFom>
                <nokkelTom>${tidsstempelMikroOppdrag.format(avstemmingsnoekkel.toNorskTid())}</nokkelTom>
                <avleverendeAvstemmingId>${uuid.value}</avleverendeAvstemmingId>
                <brukerId>BARNEPE</brukerId>
            </aksjon>
            <total>
                <totalAntall>1</totalAntall>
                <totalBelop>10000</totalBelop>
                <fortegn>T</fortegn>
            </total>
            <periode>
                <datoAvstemtFom>${tidsstempelTimeOppdrag.format(fra.toNorskTid())}</datoAvstemtFom>
                <datoAvstemtTom>${tidsstempelTimeOppdrag.format(til.toNorskTid().minusHours(1))}</datoAvstemtTom>
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
                <meldingKode>KodeMelding</meldingKode>
                <alvorlighetsgrad>Beskrivelse</alvorlighetsgrad>
                <tekstMelding>12</tekstMelding>
                <tidspunkt>${tidsstempelMikroOppdrag.format(avstemmingsnoekkel.toNorskTid())}</tidspunkt>
            </detalj>
        </Avstemmingsdata>
        
    """.trimIndent()
}