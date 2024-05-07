package no.nav.etterlatte.utbetaling.iverksetting.oppdrag

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.utbetaling.common.toXMLDate
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import no.nav.etterlatte.utbetaling.utbetaling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class OppdragMapperTest {
    @Test
    fun oppdragFraVedtak() {
        val oppdrag =
            OppdragMapper.oppdragFraUtbetaling(
                utbetaling(sakType = Saktype.BARNEPENSJON),
                erFoersteUtbetalingPaaSak = true,
                erGRegulering = false,
            )
        assertEquals(oppdrag.oppdrag110.kodeFagomraade, "BARNEPE")
        assertEquals(oppdrag.oppdrag110.oppdragsLinje150.first().kodeKlassifik, "BARNEPENSJON-OPTP")
    }

    @Test
    fun oppdragFraVedtakOMS() {
        val oppdrag =
            OppdragMapper.oppdragFraUtbetaling(
                utbetaling = utbetaling(sakType = Saktype.OMSTILLINGSSTOENAD),
                erFoersteUtbetalingPaaSak = true,
                erGRegulering = false,
            )
        assertEquals(oppdrag.oppdrag110.kodeFagomraade, "OMSTILL")
        assertEquals(oppdrag.oppdrag110.oppdragsLinje150.first().kodeKlassifik, "OMSTILLINGOR")
    }

    @Nested
    inner class OppdragFraVedtakForRegulering {
        val oppdrag =
            OppdragMapper.oppdragFraUtbetaling(
                utbetaling = utbetaling(sakType = Saktype.OMSTILLINGSSTOENAD),
                erFoersteUtbetalingPaaSak = true,
                erGRegulering = true,
            )

        @Test
        fun `Skal inneholde en fra og med dato tilsvarende vedtaket`() {
            oppdrag.oppdrag110.tekst140.single().datoTekstFom shouldBe LocalDate.parse("2022-01-01").toXMLDate()
        }

        @Test
        fun `Skal inneholde til og med dato for tekst som er 20 i utbetalingsmaaned`() {
            oppdrag.oppdrag110.tekst140.single().datoTekstTom shouldBe LocalDate.parse("2022-02-20").toXMLDate()
        }

        @Test
        fun `Skal inneholde riktig tekst`() {
            oppdrag.oppdrag110.tekst140.single().tekst shouldBe
                "Grunnbeløpet har økt fra 1. mai 2022. De aller fleste vil få etterbetalt i juni."
        }
    }
}
