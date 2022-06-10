package no.nav.etterlatte.oppgave

import no.nav.etterlatte.database.toList
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import java.util.UUID
import javax.sql.DataSource

enum class Rolle(){
    SAKSBEHANDLER, ATTESTANT
}

data class Oppgave(
    val behandlingId: UUID,
    val behandlingStatus: BehandlingStatus,
    val sakId: Long
)

class OppgaveDao(private val datasource: DataSource) {

    fun finnOppgaverForRoller(roller: List<Rolle>): List<Oppgave>{
        val aktuelleStatuser = roller.flatMap { when(it){
            Rolle.SAKSBEHANDLER -> listOf(BehandlingStatus.UNDER_BEHANDLING, BehandlingStatus.GYLDIG_SOEKNAD)
            Rolle.ATTESTANT -> listOf(BehandlingStatus.FATTET_VEDTAK)
        } }.distinct()

        if (aktuelleStatuser.isEmpty()) return emptyList()

        datasource.connection.use {
            val stmt =  it.prepareStatement(
                    "SELECT id, sak_id, behandling_opprettet, sist_endret, " +
                            "soekand_mottatt_dato, innsender, soeker, gjenlevende, avdoed, soesken, " +
                            "gyldighetssproving, status FROM behandling where status in ${aktuelleStatuser.joinToString(separator = ", ", prefix = "(", postfix = ")") { "'${it.name}'" }}"
                )
            return stmt.executeQuery().toList { Oppgave(getObject("id") as UUID, BehandlingStatus.valueOf(getString("status")), getLong("sak_id")) }
        }
    }



}