package common

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.common.tidligsteIverksatteVirkningstidspunkt
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Month
import java.time.YearMonth

class TidligsteIverksatteVirkningstidspunktKtTest {
    @Test
    fun `ser kun på iverksatte saker`() {
        val behandlingAvbrutt =
            mockBehandlingMedStatusOgVirkdato(
                BehandlingStatus.AVBRUTT,
                YearMonth.of(2022, Month.AUGUST),
            )
        val behandlingIverksatt =
            mockBehandlingMedStatusOgVirkdato(
                BehandlingStatus.IVERKSATT,
                YearMonth.of(2023, Month.JANUARY),
            )
        val tidligsteVirk = listOf(behandlingIverksatt, behandlingAvbrutt).tidligsteIverksatteVirkningstidspunkt()?.dato
        Assertions.assertEquals(YearMonth.of(2023, Month.JANUARY), tidligsteVirk)
    }

    @Test
    fun `tar med tidligste hvis det er flere iverksatte`() {
        val behandlingTidlig =
            mockBehandlingMedStatusOgVirkdato(
                BehandlingStatus.IVERKSATT,
                YearMonth.of(2000, Month.JANUARY),
            )
        val behandlingSen =
            mockBehandlingMedStatusOgVirkdato(
                BehandlingStatus.IVERKSATT,
                YearMonth.of(2022, Month.JANUARY),
            )
        val behandlingImellom =
            mockBehandlingMedStatusOgVirkdato(
                BehandlingStatus.IVERKSATT,
                YearMonth.of(2020, Month.APRIL),
            )

        Assertions.assertEquals(
            YearMonth.of(2000, Month.JANUARY),
            listOf(behandlingTidlig, behandlingSen, behandlingImellom).tidligsteIverksatteVirkningstidspunkt()?.dato,
        )
        Assertions.assertEquals(
            YearMonth.of(2000, Month.JANUARY),
            listOf(behandlingTidlig, behandlingImellom).tidligsteIverksatteVirkningstidspunkt()?.dato,
        )
        Assertions.assertEquals(
            YearMonth.of(2020, Month.APRIL),
            listOf(behandlingSen, behandlingImellom).tidligsteIverksatteVirkningstidspunkt()?.dato,
        )
        Assertions.assertEquals(
            YearMonth.of(2022, Month.JANUARY),
            listOf(behandlingSen).tidligsteIverksatteVirkningstidspunkt()?.dato,
        )
    }

    @Test
    fun `gir null hvis ingen iverksatte behandlinger`() {
        val behandlingAttestert =
            mockBehandlingMedStatusOgVirkdato(
                BehandlingStatus.ATTESTERT,
                YearMonth.of(2022, Month.APRIL),
            )
        val behandlingReturnert =
            mockBehandlingMedStatusOgVirkdato(
                BehandlingStatus.RETURNERT,
                YearMonth.of(2023, Month.MAY),
            )
        val behandlingUtenVirk =
            mockBehandlingMedStatusOgVirkdato(
                BehandlingStatus.OPPRETTET,
            )
        Assertions.assertNull(listOf(behandlingAttestert, behandlingReturnert).tidligsteIverksatteVirkningstidspunkt())
        Assertions.assertNull(emptyList<Behandling>().tidligsteIverksatteVirkningstidspunkt())
        Assertions.assertNull(listOf(behandlingUtenVirk).tidligsteIverksatteVirkningstidspunkt())
        Assertions.assertNull(
            listOf(behandlingUtenVirk, behandlingAttestert, behandlingReturnert).tidligsteIverksatteVirkningstidspunkt(),
        )
    }

    private fun mockBehandlingMedStatusOgVirkdato(
        behandlingStatus: BehandlingStatus,
        virkDato: YearMonth? = null,
    ): Behandling {
        return mockk<Behandling> {
            every { status } returns behandlingStatus
            if (virkDato != null) {
                every { virkningstidspunkt } returns
                    mockk<Virkningstidspunkt> {
                        every { dato } returns virkDato
                    }
            } else {
                every { virkningstidspunkt } returns null
            }
        }
    }
}
