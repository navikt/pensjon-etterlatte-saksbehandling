package no.nav.etterlatte.oppgave

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.etterlatte.TRIVIELL_MIDTPUNKT
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.oppgave.domain.Oppgave
import org.junit.jupiter.api.Test
import java.util.*

internal class OppgaveServiceTest {
    @Test
    fun `filter oppgaveliste for enheter`() {
        val oppgaveListe: List<Oppgave> = listOf(
            Oppgave.BehandlingOppgave(
                sakId = 1,
                sakType = SakType.BARNEPENSJON,
                registrertDato = Tidspunkt.now(),
                fnr = TRIVIELL_MIDTPUNKT,
                behandlingId = UUID.randomUUID(),
                behandlingsType = BehandlingType.FØRSTEGANGSBEHANDLING,
                behandlingStatus = BehandlingStatus.OPPRETTET,
                antallSoesken = 1,
                enhet = null
            ),
            Oppgave.BehandlingOppgave(
                sakId = 2,
                sakType = SakType.BARNEPENSJON,
                registrertDato = Tidspunkt.now(),
                fnr = TRIVIELL_MIDTPUNKT,
                behandlingId = UUID.randomUUID(),
                behandlingsType = BehandlingType.FØRSTEGANGSBEHANDLING,
                behandlingStatus = BehandlingStatus.OPPRETTET,
                antallSoesken = 1,
                enhet = Enheter.DEFAULT.enhetNr
            ),
            Oppgave.BehandlingOppgave(
                sakId = 3,
                sakType = SakType.BARNEPENSJON,
                registrertDato = Tidspunkt.now(),
                fnr = TRIVIELL_MIDTPUNKT,
                behandlingId = UUID.randomUUID(),
                behandlingsType = BehandlingType.FØRSTEGANGSBEHANDLING,
                behandlingStatus = BehandlingStatus.OPPRETTET,
                antallSoesken = 1,
                enhet = Enheter.STRENGT_FORTROLIG.enhetNr
            ),
            Oppgave.Grunnlagsendringsoppgave(
                sakId = 4,
                sakType = SakType.BARNEPENSJON,
                registrertDato = Tidspunkt.now(),
                fnr = TRIVIELL_MIDTPUNKT,
                grunnlagsendringsType = GrunnlagsendringsType.FORELDER_BARN_RELASJON,
                gjelderRolle = Saksrolle.SOEKER
            )
        )

        val enhetsListe = listOf(Enheter.DEFAULT.enhetNr, Enheter.EGNE_ANSATTE.enhetNr)

        val service = OppgaveServiceImpl(mockk())

        val filtered = service.filterOppgaverForEnheter(oppgaveListe, enhetsListe)

        filtered.size shouldBe 3
        filtered.filter { it.sakId == 3L }.size shouldBe 0
    }
}