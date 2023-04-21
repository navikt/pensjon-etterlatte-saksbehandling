package no.nav.etterlatte.oppgave

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.TRIVIELL_MIDTPUNKT
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.oppgave.domain.Oppgave
import org.junit.jupiter.api.Test

internal class OppgaveDTOTest {

    @Test
    fun `Skal opprette merknad paa grunnlagsendringsoppgaver`() {
        Oppgave.Grunnlagsendringsoppgave(
            sak = Sak(
                id = 4,
                sakType = SakType.BARNEPENSJON,
                ident = TRIVIELL_MIDTPUNKT.value,
                enhet = null
            ),
            registrertDato = Tidspunkt.now(),
            grunnlagsendringsType = GrunnlagsendringsType.DOEDSFALL,
            gjelderRolle = Saksrolle.SOEKER
        )
            .let { OppgaveDTO.fraOppgave(it) }
            .let { it.merknad shouldBe "Mottaker er registrert død" }

        Oppgave.Grunnlagsendringsoppgave(
            sak = Sak(
                id = 4,
                sakType = SakType.BARNEPENSJON,
                ident = TRIVIELL_MIDTPUNKT.value,
                enhet = null
            ),
            registrertDato = Tidspunkt.now(),
            grunnlagsendringsType = GrunnlagsendringsType.GRUNNBELOEP,
            gjelderRolle = Saksrolle.SOEKER
        )
            .let { OppgaveDTO.fraOppgave(it) }
            .let { it.merknad shouldBe "Endring i grunnbeløp" }
    }
}