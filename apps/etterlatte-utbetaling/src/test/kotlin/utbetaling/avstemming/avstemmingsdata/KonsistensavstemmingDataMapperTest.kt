package no.nav.etterlatte.utbetaling.avstemming.avstemmingsdata

import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import no.nav.etterlatte.utbetaling.mockKonsistensavstemming
import no.nav.etterlatte.utbetaling.oppdragForKonsistensavstemming
import no.nav.etterlatte.utbetaling.oppdragslinjeForKonsistensavstemming
import no.nav.virksomhet.tjenester.avstemming.informasjon.konsistensavstemmingsdata.v1.Konsistensavstemmingsdata
import no.nav.virksomhet.tjenester.avstemming.informasjon.konsistensavstemmingsdata.v1.Oppdragsdata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

internal class KonsistensavstemmingDataMapperTest {

    @Test
    fun `konsistensavstemming uten noe aa avstemme skal ikke inneholde data`() {
        val konsistensavstemming = mockKonsistensavstemming(loependeUtbetalinger = emptyList())

        val dataMapper = KonsistensavstemmingDataMapper(konsistensavstemming)
        val opprettetAvstemmingsmelding = dataMapper.opprettAvstemmingsmelding(Saktype.BARNEPENSJON)

        assertDoesNotThrow { `liste av konsistensavstemmingsdata har rett struktur`(opprettetAvstemmingsmelding) }
        assertEquals(emptyList<Oppdragsdata>(), opprettetAvstemmingsmelding[1].oppdragsdataListe)
        assertEquals(BigInteger.ZERO, opprettetAvstemmingsmelding[1].totaldata.totalAntall)
        assertEquals(BigDecimal.ZERO, opprettetAvstemmingsmelding[1].totaldata.totalBelop)
        assertEquals("T", opprettetAvstemmingsmelding[1].totaldata.fortegn)
    }

    @Test
    fun `skal mappe konsistensavstemming med en utbetalingslinje`() {
        val oppdragslinjer = listOf(oppdragslinjeForKonsistensavstemming(fraOgMed = LocalDate.of(2022, 10, 7)))
        val oppdrag = oppdragForKonsistensavstemming(oppdragslinjeForKonsistensavstemming = oppdragslinjer)
        val konsistensavstemming = mockKonsistensavstemming(loependeUtbetalinger = listOf(oppdrag))

        val dataMapper = KonsistensavstemmingDataMapper(konsistensavstemming)
        val opprettetAvstemmingsmelding = dataMapper.opprettAvstemmingsmelding(Saktype.BARNEPENSJON)
        assertDoesNotThrow { `liste av konsistensavstemmingsdata har rett struktur`(opprettetAvstemmingsmelding) }
        assertEquals(1, opprettetAvstemmingsmelding[1].oppdragsdataListe.size)
        assertEquals(oppdrag.fnr.value, opprettetAvstemmingsmelding[1].oppdragsdataListe.first().oppdragGjelderId)
        assertEquals(1, opprettetAvstemmingsmelding[1].oppdragsdataListe.first().oppdragslinjeListe.size)
        assertEquals(
            oppdragslinjer.first().id.value.toString(),
            opprettetAvstemmingsmelding[1].oppdragsdataListe.first().oppdragslinjeListe.first().delytelseId
        )
        assertEquals(
            BigDecimal.valueOf(10000L),
            opprettetAvstemmingsmelding[1].oppdragsdataListe.first().oppdragslinjeListe.first().sats
        )
    }

    @Test
    fun `skal mappe konsistensavstememig for flere utbetalinger og flere utbetalingslinjer`() {
        val sakId1 = 1L
        val oppdragslinjer1 = listOf(
            oppdragslinjeForKonsistensavstemming(fraOgMed = LocalDate.of(2021, 1, 1))
        )
        val oppdrag1 =
            oppdragForKonsistensavstemming(sakId = sakId1, oppdragslinjeForKonsistensavstemming = oppdragslinjer1)

        val oppdragslinjer2 = listOf(
            oppdragslinjeForKonsistensavstemming(
                id = 1,
                fraOgMed = LocalDate.of(2022, 1, 1)
            ),
            oppdragslinjeForKonsistensavstemming(
                id = 2,
                fraOgMed = LocalDate.of(2022, 7, 1),
                forrigeUtbetalingslinjeId = 1
            )
        )
        val sakId2 = 2L
        val oppdrag2 =
            oppdragForKonsistensavstemming(sakId = sakId2, oppdragslinjeForKonsistensavstemming = oppdragslinjer2)

        val konsistensavstemming = mockKonsistensavstemming(loependeUtbetalinger = listOf(oppdrag1, oppdrag2))
        val dataMapper = KonsistensavstemmingDataMapper(konsistensavstemming)
        val opprettetAvstemmingsmelding = dataMapper.opprettAvstemmingsmelding(Saktype.BARNEPENSJON)

        assertDoesNotThrow { `liste av konsistensavstemmingsdata har rett struktur`(opprettetAvstemmingsmelding) }

        val sakIderIKonsistensavstemming =
            opprettetAvstemmingsmelding[1].oppdragsdataListe.map { it.fagsystemId }.toSet()
        assertEquals(
            setOf(sakId1.toString(), sakId2.toString()),
            sakIderIKonsistensavstemming
        )

        val oppdragsdataForSak1 =
            opprettetAvstemmingsmelding[1].oppdragsdataListe.find { it.fagsystemId == sakId1.toString() }
        val oppdragsdataForSak2 =
            opprettetAvstemmingsmelding[1].oppdragsdataListe.find { it.fagsystemId == sakId2.toString() }
        val utbetalingslinjerForSak1 = oppdragsdataForSak1!!.oppdragslinjeListe.map { it.delytelseId }
        val utbetalingslinjerForSak2 = oppdragsdataForSak2!!.oppdragslinjeListe.map { it.delytelseId }

        assertEquals(oppdragslinjer1.map { it.id.value.toString() }.toSet(), utbetalingslinjerForSak1.toSet())
        assertEquals(oppdragslinjer2.map { it.id.value.toString() }.toSet(), utbetalingslinjerForSak2.toSet())

        val totaldata = opprettetAvstemmingsmelding.find { it.totaldata != null }!!.totaldata
        assertEquals(BigDecimal.valueOf(30000L), totaldata!!.totalBelop)
    }
}

fun `liste av konsistensavstemmingsdata har rett struktur`(konsistensavstemmingsdata: List<Konsistensavstemmingsdata>) {
    if (konsistensavstemmingsdata.size < 3) {
        throw IllegalArgumentException("Konsistensavstemmingsdata skal ha tre eller flere elementer")
    }
    if (konsistensavstemmingsdata.first().aksjonsdata.aksjonsType != "START") {
        throw IllegalArgumentException("Aksjonstypen til foerste datamelding var ikke START")
    }
    if (konsistensavstemmingsdata.last().aksjonsdata.aksjonsType != "AVSL") {
        throw IllegalArgumentException("Aksjonstypen til siste datamelding var ikke AVSL")
    }

    if (konsistensavstemmingsdata.subList(1, konsistensavstemmingsdata.size - 1)
            .any { it.aksjonsdata.aksjonsType != "DATA" }
    ) {
        throw IllegalArgumentException(
            "Aksjonstypen til en eller flere av meldingene mellom START og AVSL var ikke DATA"
        )
    }
}