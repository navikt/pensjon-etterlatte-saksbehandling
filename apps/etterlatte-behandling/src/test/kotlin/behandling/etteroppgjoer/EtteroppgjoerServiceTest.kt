package no.nav.etterlatte.behandling.etteroppgjoer

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.Test

class EtteroppgjoerServiceTest {
    private val dao: EtteroppgjoerDao = mockk()
    val sakService: SakService = mockk()
    private val etteroppgjoerService: EtteroppgjoerService = EtteroppgjoerService(dao, mockk(), sakService, mockk())

    @Test
    fun `skal oppdatere status hvis sak skal ha etteroppgjør`() {
        val sak =
            mockk<Sak> {
                every { id } returns SakId(1)
            }
        every { sakService.finnSak(any(), any()) } returns sak

        // ikke opprett etteroppgjør
        every { etteroppgjoerService.hentEtteroppgjoer(any(), any()) } returns null
        etteroppgjoerService.skalHaEtteroppgjoer("123", 2024).skalHaEtteroppgjoer shouldBe false

        val etteroppgjoer = mockk<Etteroppgjoer>()
        every { etteroppgjoerService.hentEtteroppgjoer(any(), any()) } returns etteroppgjoer

        // skal ha etteroppgjør
        val statusSomSkalHaEtteroppgjoer =
            setOf(
                EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
                EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER,
            )

        enumValues<EtteroppgjoerStatus>().forEach { status ->
            every { etteroppgjoer.status } returns status
            val forventet = status in statusSomSkalHaEtteroppgjoer
            etteroppgjoerService.skalHaEtteroppgjoer("123", 2024).skalHaEtteroppgjoer shouldBe forventet
        }
    }
}
