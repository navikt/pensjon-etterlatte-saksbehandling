package no.nav.etterlatte.oppdrag

import no.nav.etterlatte.dummyAttestasjon
import no.nav.etterlatte.dummyVedtak
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class OppdragMapperTest {

    @Test
    fun oppdragFraVedtak() {
        val oppdrag = OppdragMapper.oppdragFraVedtak(dummyVedtak(), dummyAttestasjon())

        assertNotNull(oppdrag)
    }

}