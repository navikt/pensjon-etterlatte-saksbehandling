package no.nav.etterlatte.oppdrag

import no.nav.etterlatte.attestasjon
import no.nav.etterlatte.vedtak
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