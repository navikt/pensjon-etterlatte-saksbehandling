package no.nav.etterlatte.behandling.etteroppgjoer

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.Test

class EtteroppgjoerServiceTest {
    val sakService: SakService = mockk()

    @Test
    fun `skal ha etteroppgjoer hvis status`() {
        // skal ha etteroppgjør, oppdater status
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
