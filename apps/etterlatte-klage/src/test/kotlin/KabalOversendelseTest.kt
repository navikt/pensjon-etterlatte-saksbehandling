package no.nav.etterlatte.klage.modell

import no.nav.etterlatte.libs.common.behandling.KabalHjemmel
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class KabalOversendelseTest {
    @Test
    fun `kan mappe alle KabalHjemmel til Hjemmel i klagekodeverk`() {
        Assertions.assertDoesNotThrow {
            KabalHjemmel.entries.forEach {
                it.tilHjemmel()
            }
        }
    }
}
