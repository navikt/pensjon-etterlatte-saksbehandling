package no.nav.etterlatte.oppgave

import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

data class OppgaveEndring(
    val id: UUID,
    val oppgaveId: UUID,
    val tidspunkt: Tidspunkt,
    val saksbehandler: String?,
    val status: Status,
    val merknad: String?,
    val enhet: String?,
    val kilde: String?,
    val type: EndringType,
)

enum class EndringType {
    OPPRETTET_OPPGAVE,
    ENDRET_TILDELING,
    FJERN_TILDELING,
    ENDRET_STATUS,
    ENDRET_STATUS_OG_MERKNAD,
    ENDRET_ENHET,
    ENDRET_FRIST,
    ENDRET_MERKNAD,
    ENDRET_KILDE,
}
