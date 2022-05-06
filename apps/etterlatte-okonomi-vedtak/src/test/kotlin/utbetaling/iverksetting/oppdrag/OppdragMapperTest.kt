package no.nav.etterlatte.utbetaling.iverksetting.oppdrag

import no.nav.etterlatte.utbetaling.attestasjon
import no.nav.etterlatte.utbetaling.vedtak
import no.nav.su.se.bakover.common.Tidspunkt
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class OppdragMapperTest {

    @Test
    fun oppdragFraVedtak() {
        val oppdrag = OppdragMapper.oppdragFraVedtak(vedtak(), attestasjon(), Tidspunkt.now())

        assertNotNull(oppdrag)
    }

}