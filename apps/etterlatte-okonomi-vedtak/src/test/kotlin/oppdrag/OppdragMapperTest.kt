package no.nav.etterlatte.oppdrag

import no.nav.etterlatte.mockVedtak
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class OppdragMapperTest {

    @Test
    fun oppdragFraVedtak() {
        val oppdrag = OppdragMapper.oppdragFraVedtak(mockVedtak())

        assertNotNull(oppdrag)
    }

}