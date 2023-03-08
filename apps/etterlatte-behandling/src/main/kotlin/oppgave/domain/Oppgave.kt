package no.nav.etterlatte.oppgave.domain

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringsType
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDate
import java.util.*

enum class Handling {
    BEHANDLE, GAA_TIL_SAK
}

sealed class Oppgave {
    abstract val sakId: Long
    abstract val sakType: SakType
    abstract val registrertDato: Tidspunkt
    abstract val fristDato: LocalDate
    abstract val fnr: Foedselsnummer
    abstract val handling: Handling
    abstract val oppgaveStatus: OppgaveStatus
    abstract val oppgaveType: OppgaveType

    data class BehandlingOppgave(
        override val sakId: Long,
        override val sakType: SakType,
        override val registrertDato: Tidspunkt,
        override val fnr: Foedselsnummer,
        val behandlingId: UUID,
        val behandlingsType: BehandlingType,
        val behandlingStatus: BehandlingStatus,
        val antallSoesken: Int
    ) : Oppgave() {
        override val oppgaveStatus: OppgaveStatus
            get() = OppgaveStatus.from(behandlingStatus)

        override val handling: Handling
            get() = Handling.BEHANDLE

        override val fristDato: LocalDate
            get() = registrertDato.toLocalDate().plusMonths(1)

        override val oppgaveType: OppgaveType
            get() = OppgaveType.fraBehandlingsType(behandlingsType)
    }

    data class Grunnlagsendringsoppgave(
        override val sakId: Long,
        override val sakType: SakType,
        override val registrertDato: Tidspunkt,
        override val fnr: Foedselsnummer,
        val grunnlagsendringsType: GrunnlagsendringsType,
        val gjelderRolle: Saksrolle
    ) : Oppgave() {
        override val oppgaveStatus: OppgaveStatus
            get() = OppgaveStatus.NY

        override val handling: Handling
            get() = Handling.GAA_TIL_SAK

        override val fristDato: LocalDate
            get() = registrertDato.toLocalDate().plusMonths(1)

        override val oppgaveType: OppgaveType
            get() = OppgaveType.ENDRING_PAA_SAK

        // På sikt kan vi vurdere om dette heller bør være strukturerte data som går til frontend, og
        // utledes til en menneskelig lesbar tekst der i stedet. 07.02.2023 ØSH
        val beskrivelse: String
            get() = "Endring av $grunnlagsendringsType for $gjelderRolle".lowercase()
                .replaceFirstChar { it.uppercase() }
    }
}