import no.nav.etterlatte.libs.common.oppgave.OppgaveSaksbehandler
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt

data class GosysOppgave(
    val id: Long,
    val versjon: Long,
    val status: String,
    val tema: String,
    val oppgavetype: String,
    val saksbehandler: OppgaveSaksbehandler?,
    val enhet: String,
    val opprettet: Tidspunkt,
    val frist: Tidspunkt?,
    val fnr: String? = null,
    val beskrivelse: String?,
    val journalpostId: String?,
)
