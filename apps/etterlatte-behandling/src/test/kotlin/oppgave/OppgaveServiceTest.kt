package no.nav.etterlatte.oppgave

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.Saksbehandler
import no.nav.etterlatte.TRIVIELL_MIDTPUNKT
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.oppgave.domain.Oppgave
import org.junit.jupiter.api.Test
import java.util.*

internal class OppgaveServiceTest {
    private val oppgaveListe: List<Oppgave> = listOf(
        Oppgave.BehandlingOppgave(
            sak = Sak(
                id = 1,
                sakType = SakType.BARNEPENSJON,
                ident = TRIVIELL_MIDTPUNKT.value,
                enhet = null
            ),
            registrertDato = Tidspunkt.now(),
            behandlingId = UUID.randomUUID(),
            behandlingsType = BehandlingType.FØRSTEGANGSBEHANDLING,
            behandlingStatus = BehandlingStatus.OPPRETTET,
            merknad = null
        ),
        Oppgave.BehandlingOppgave(
            sak = Sak(
                id = 2,
                sakType = SakType.BARNEPENSJON,
                ident = TRIVIELL_MIDTPUNKT.value,
                enhet = Enheter.PORSGRUNN.enhetNr
            ),
            registrertDato = Tidspunkt.now(),
            behandlingId = UUID.randomUUID(),
            behandlingsType = BehandlingType.FØRSTEGANGSBEHANDLING,
            behandlingStatus = BehandlingStatus.OPPRETTET,
            merknad = null
        ),
        Oppgave.BehandlingOppgave(
            sak = Sak(
                id = 3,
                sakType = SakType.BARNEPENSJON,
                ident = TRIVIELL_MIDTPUNKT.value,
                enhet = Enheter.STRENGT_FORTROLIG.enhetNr
            ),
            registrertDato = Tidspunkt.now(),
            behandlingId = UUID.randomUUID(),
            behandlingsType = BehandlingType.FØRSTEGANGSBEHANDLING,
            behandlingStatus = BehandlingStatus.OPPRETTET,
            merknad = null
        ),
        Oppgave.Grunnlagsendringsoppgave(
            sak = Sak(
                id = 4,
                sakType = SakType.BARNEPENSJON,
                ident = TRIVIELL_MIDTPUNKT.value,
                enhet = null
            ),
            registrertDato = Tidspunkt.now(),
            grunnlagsendringsType = GrunnlagsendringsType.FORELDER_BARN_RELASJON,
            gjelderRolle = Saksrolle.SOEKER
        )
    )

    @Test
    fun `filter oppgaveliste for enheter`() {
        val featureToggleService = mockk<FeatureToggleService>()
        every { featureToggleService.isEnabled(any(), false) } returns true

        val user = mockk<Saksbehandler>()

        every { user.enheter() } returns listOf(
            Enheter.PORSGRUNN.enhetNr,
            Enheter.EGNE_ANSATTE.enhetNr
        )

        val filtered = oppgaveListe.filterOppgaverForEnheter(featureToggleService, user)

        filtered.size shouldBe 3
        filtered.filter { it.sak.id == 3L }.size shouldBe 0
    }
}