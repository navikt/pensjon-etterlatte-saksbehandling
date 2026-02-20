package no.nav.etterlatte.behandling.revurdering

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.ViderefoertOpphoer
import no.nav.etterlatte.behandling.domain.OpphoerFraTidligereBehandling
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import no.nav.etterlatte.opprettBehandling
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

class RevurderingServiceTest {
    @Test
    fun `kopierViderefoertOpphoer skal kaste exception hvis ny opphørsdato er forskjellig fra tidligere videreført opphør`() {
        val behandlingDao = mockk<BehandlingDao>()

        val revurderingService =
            RevurderingService(
                oppgaveService = mockk(),
                grunnlagService = mockk(),
                behandlingHendelser = mockk(),
                behandlingDao = behandlingDao,
                hendelseDao = mockk(),
                kommerBarnetTilGodeService = mockk(),
                revurderingDao = mockk(),
                aktivitetspliktDao = mockk(),
                aktivitetspliktKopierService = mockk(),
            )

        val tidligereBehandlingId = UUID.randomUUID()
        every { behandlingDao.hentViderefoertOpphoer(tidligereBehandlingId) } returns
            viderefoertOpphoer(
                behandlingId = tidligereBehandlingId,
                opphoersdato = YearMonth.of(2018, 2),
            )

        shouldThrow<InternfeilException> {
            revurderingService.kopierViderefoertOpphoer(
                opprettBehandling =
                    opprettBehandling(
                        type = BehandlingType.REVURDERING,
                        sakId = randomSakId(),
                    ),
                opphoerFraTidligereBehandling = OpphoerFraTidligereBehandling(YearMonth.of(2018, 1), tidligereBehandlingId),
                saksbehandlerIdent = "JABO",
            )
        }
    }

    private fun viderefoertOpphoer(
        behandlingId: UUID,
        opphoersdato: YearMonth,
    ): ViderefoertOpphoer =
        ViderefoertOpphoer(
            JaNei.JA,
            behandlingId,
            opphoersdato,
            VilkaarType.BP_ALDER_BARN,
            "begrunnelse",
            kilde,
            true,
        )
}
