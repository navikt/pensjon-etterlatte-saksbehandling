package no.nav.etterlatte.oppgave

import no.nav.etterlatte.database.toList
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import no.nav.etterlatte.sak.Sak
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import javax.sql.DataSource

enum class Rolle() {
    SAKSBEHANDLER, ATTESTANT
}

data class Oppgave(
    val behandlingId: UUID,
    val behandlingStatus: BehandlingStatus,
    val oppgaveStatus: OppgaveStatus,
    val sak: Sak,
    val regdato: ZonedDateTime,
    val fristDato: LocalDate
)

class OppgaveDao(private val datasource: DataSource) {

    fun finnOppgaverForRoller(roller: List<Rolle>): List<Oppgave> {
        val aktuelleStatuser = roller.flatMap {
            when (it) {
                Rolle.SAKSBEHANDLER -> listOf(
                    BehandlingStatus.UNDER_BEHANDLING,
                    BehandlingStatus.GYLDIG_SOEKNAD,
                    BehandlingStatus.RETURNERT
                )
                Rolle.ATTESTANT -> listOf(BehandlingStatus.FATTET_VEDTAK)
            }
        }.distinct()

        if (aktuelleStatuser.isEmpty()) return emptyList()

        datasource.connection.use {
            val stmt = it.prepareStatement(
                """
                |SELECT b.id, b.sak_id, soekand_mottatt_dato, fnr, sakType, status, oppgave_status, behandling_opprettet
                |FROM behandling b inner join sak s on b.sak_id = s.id  
                |where status in ${aktuelleStatuser.joinToString(separator = ", ", prefix = "(", postfix = ")") { "'${it.name}'" }}
                """.trimMargin().also {
                    println(it)
                }
            )
            return stmt.executeQuery().toList {
                val mottattDato = getTimestamp("soekand_mottatt_dato")?.toLocalDateTime()?.atZone(ZoneId.of("UTC"))
                    ?: getTimestamp("behandling_opprettet")?.toLocalDateTime()?.atZone(ZoneId.of("UTC"))
                    ?: throw IllegalStateException(
                        "Vi har en behandling som hverken har soekand mottatt dato eller behandling opprettet dato "
                    )
                Oppgave(
                    getObject("id") as UUID,
                    BehandlingStatus.valueOf(getString("status")),
                    OppgaveStatus.valueOf(getString("oppgave_status")),
                    Sak(getString("fnr"), getString("sakType"), getLong("sak_id")),
                    mottattDato,
                    mottattDato.toLocalDate().plusMonths(1)
                )
            }.also {
                println("""Hentet oppgaveliste for bruker med roller $roller. Fant ${it.size} oppgaver""")
            }
        }
    }
}