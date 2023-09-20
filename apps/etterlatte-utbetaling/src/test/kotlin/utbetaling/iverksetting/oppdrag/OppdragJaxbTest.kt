package no.nav.etterlatte.utbetaling.iverksetting.oppdrag

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.utbetaling.common.toUUID30
import no.nav.etterlatte.utbetaling.readFile
import no.nav.etterlatte.utbetaling.utbetaling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID.randomUUID

internal class OppdragJaxbTest {
    @Test
    fun `should generate xml from oppdrag`() {
        val behandlingId = randomUUID()
        val now = Tidspunkt.ofNorskTidssone(LocalDate.parse("2023-01-01"), LocalTime.of(0, 0, 0))
        val oppdrag =
            OppdragMapper.oppdragFraUtbetaling(
                utbetaling(behandlingId = behandlingId, avstemmingsnoekkel = now, opprettet = now),
                true,
            )
        val oppdragAsXml = OppdragJaxb.toXml(oppdrag)

        val gyldigOppdragXml =
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <ns2:oppdrag xmlns:ns2="http://www.trygdeetaten.no/skjema/oppdrag">
                <oppdrag-110>
                    <kodeAksjon>1</kodeAksjon>
                    <kodeEndring>NY</kodeEndring>
                    <kodeFagomraade>BARNEPE</kodeFagomraade>
                    <fagsystemId>1</fagsystemId>
                    <utbetFrekvens>MND</utbetFrekvens>
                    <oppdragGjelderId>12345678903</oppdragGjelderId>
                    <datoOppdragGjelderFom>1970-01-01</datoOppdragGjelderFom>
                    <saksbehId>12345678</saksbehId>
                    <avstemming-115>
                        <kodeKomponent>ETTERLAT</kodeKomponent>
                        <nokkelAvstemming>2023-01-01-00.00.00.000000</nokkelAvstemming>
                        <tidspktMelding>2023-01-01-00.00.00.000000</tidspktMelding>
                    </avstemming-115>
                    <oppdrags-enhet-120>
                        <typeEnhet>BOS</typeEnhet>
                        <enhet>4819</enhet>
                        <datoEnhetFom>1900-01-01</datoEnhetFom>
                    </oppdrags-enhet-120>
                    <oppdrags-linje-150>
                        <kodeEndringLinje>NY</kodeEndringLinje>
                        <vedtakId>1</vedtakId>
                        <delytelseId>1</delytelseId>
                        <kodeKlassifik>BARNEPENSJON-OPTP</kodeKlassifik>
                        <datoVedtakFom>2022-01-01</datoVedtakFom>
                        <sats>10000</sats>
                        <fradragTillegg>T</fradragTillegg>
                        <typeSats>MND</typeSats>
                        <brukKjoreplan>N</brukKjoreplan>
                        <saksbehId>12345678</saksbehId>
                        <utbetalesTilId>12345678903</utbetalesTilId>
                        <henvisning>${behandlingId.toUUID30().value}</henvisning>
                        <attestant-180>
                            <attestantId>87654321</attestantId>
                        </attestant-180>
                    </oppdrags-linje-150>
                </oppdrag-110>
            </ns2:oppdrag>

            """.trimIndent()

        assertEquals(gyldigOppdragXml, oppdragAsXml)
    }

    @Test
    fun `should convert xml to oppdrag`() {
        val oppdragXml = readFile("/oppdrag_ugyldig.xml")
        val oppdrag = OppdragJaxb.toOppdrag(oppdragXml)

        assertNotNull(oppdrag)
        assertEquals("08", oppdrag.mmel.alvorlighetsgrad)
    }
}
