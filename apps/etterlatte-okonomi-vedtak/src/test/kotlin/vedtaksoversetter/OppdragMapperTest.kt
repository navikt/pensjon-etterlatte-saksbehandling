package vedtaksoversetter

import dummyAttestasjon
import dummyVedtak
import no.nav.etterlatte.vedtaksoversetter.OppdragMapper
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class OppdragMapperTest {

    @Test
    fun oppdragFraVedtak() {
        val oppdrag = OppdragMapper.oppdragFraVedtak(dummyVedtak(), dummyAttestasjon())

        assertNotNull(oppdrag)
    }

}