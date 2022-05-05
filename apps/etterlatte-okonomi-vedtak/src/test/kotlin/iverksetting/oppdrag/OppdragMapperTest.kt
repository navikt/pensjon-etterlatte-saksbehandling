package no.nav.etterlatte.utbetaling.iverksetting.oppdrag

import no.nav.etterlatte.utbetaling.attestasjon
import no.nav.etterlatte.utbetaling.vedtak
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class OppdragMapperTest {

    @Test
    fun oppdragFraVedtak() {
        val oppdrag = OppdragMapper.oppdragFraVedtak(vedtak(), attestasjon(), LocalDateTime.now())

        assertNotNull(oppdrag)
    }

}