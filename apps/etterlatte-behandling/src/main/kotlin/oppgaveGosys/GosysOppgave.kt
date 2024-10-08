package no.nav.etterlatte.oppgaveGosys

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.oppgave.OppgaveSaksbehandler
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt

data class GosysOppgave(
    val id: Long,
    val versjon: Long,
    val status: String,
    val tema: String,
    val oppgavetype: String,
    val saksbehandler: OppgaveSaksbehandler?,
    val enhet: Enhetsnummer,
    val opprettet: Tidspunkt,
    val frist: Tidspunkt?,
    val beskrivelse: String?,
    val journalpostId: String?,
    val bruker: GosysOppgaveBruker?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GosysOppgaveBruker(
    val ident: String?,
    val type: Type?,
) {
    enum class Type {
        PERSON,
        ARBEIDSGIVER,
        SAMHANDLER,
    }
}
