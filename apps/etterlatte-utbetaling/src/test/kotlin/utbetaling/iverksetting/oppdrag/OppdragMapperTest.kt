package no.nav.etterlatte.utbetaling.iverksetting.oppdrag

import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import no.nav.etterlatte.utbetaling.utbetaling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class OppdragMapperTest {

    @Test
    fun oppdragFraVedtak() {
        val oppdrag = OppdragMapper.oppdragFraUtbetaling(utbetaling(sakType = Saktype.BARNEPENSJON), true)
        assertEquals(oppdrag.oppdrag110.kodeFagomraade, "BARNEPE")
    }
    @Test
    fun oppdragFraVedtakOMS() {
        val oppdrag = OppdragMapper.oppdragFraUtbetaling(utbetaling(sakType = Saktype.OMSTILLINGSSTOENAD), true)
        assertEquals(oppdrag.oppdrag110.kodeFagomraade, "OMSTILL")
    }
}