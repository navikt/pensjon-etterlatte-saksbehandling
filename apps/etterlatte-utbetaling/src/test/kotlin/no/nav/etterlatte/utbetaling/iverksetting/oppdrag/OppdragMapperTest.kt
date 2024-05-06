package no.nav.etterlatte.utbetaling.iverksetting.oppdrag

import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import no.nav.etterlatte.utbetaling.utbetaling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
}
