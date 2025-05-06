package no.nav.etterlatte.behandling.etteroppgjoer

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.sak.SakId
import org.junit.jupiter.api.Test

class EtteroppgjoerServiceTest {
    @Test
    fun `skal ha etteroppgjoer hvis status`() {
        // skal ha etteroppgjør
        val statusSomSkalHaEtteroppgjoer =
            setOf(
                EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
                EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER,
            )

        enumValues<EtteroppgjoerStatus>().forEach { status ->
            val etteroppgjoer = Etteroppgjoer(SakId(123), 2024, status)
            val forventet = status in statusSomSkalHaEtteroppgjoer
            etteroppgjoer.skalHaEtteroppgjoer() shouldBe forventet
        }
    }
}
