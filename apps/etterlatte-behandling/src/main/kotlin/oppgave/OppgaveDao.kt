package no.nav.etterlatte.oppgave

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.database.toList
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringStatus
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringsOppgave
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.sak.Sak
import java.sql.Connection
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

enum class Rolle {
    SAKSBEHANDLER, ATTESTANT
}

data class Oppgave(
    val behandlingId: UUID,
    val behandlingStatus: BehandlingStatus,
    val oppgaveStatus: OppgaveStatus,
    val sak: Sak,
    val regdato: ZonedDateTime,
    val fristDato: LocalDate,
    val behandlingsType: BehandlingType,
    val antallSoesken: Int
)

class OppgaveDao(private val connection: () -> Connection) {

    fun finnOppgaverMedStatuser(statuser: List<BehandlingStatus>): List<Oppgave> {
        if (statuser.isEmpty()) return emptyList()

        connection().use {
            val stmt = it.prepareStatement(
                """
                |SELECT b.id, b.sak_id, soekand_mottatt_dato, fnr, sakType, status, oppgave_status, behandling_opprettet,
                |behandlingstype, soesken 
                |FROM behandling b INNER JOIN sak s ON b.sak_id = s.id 
                |WHERE status = ANY(?)
                """.trimMargin()
            )
            stmt.setArray(1, it.createArrayOf("text", statuser.toTypedArray()))
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
                    Sak(getString("fnr"), enumValueOf(getString("sakType")), getLong("sak_id")),
                    mottattDato,
                    mottattDato.toLocalDate().plusMonths(1),
                    BehandlingType.valueOf(getString("behandlingstype")),
                    antallSoesken(getString("soesken"))
                )
            }.also {
                println("""Hentet oppgaveliste for bruker med statuser $statuser. Fant ${it.size} oppgaver""")
            }
        }
    }

    fun finnOppgaverFraGrunnlagsendringshendelser(): List<GrunnlagsendringsOppgave> {
        connection().use { connection ->
            val stmt = connection.prepareStatement(
                """
                SELECT g.sak_id, g.type, g.behandling_id, g.opprettet, s.fnr, s.saktype 
                FROM grunnlagsendringshendelse g 
                INNER JOIN sak s ON g.sak_id = s.id
                WHERE status = ?
                """.trimIndent()
            )
            stmt.setString(1, GrunnlagsendringStatus.GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING.name)
            return stmt.executeQuery().toList {
                GrunnlagsendringsOppgave(
                    sakId = getLong("sak_id"),
                    sakType = enumValueOf(getString("saktype")),
                    type = enumValueOf(getString("type")),
                    opprettet = getTimestamp("opprettet").toLocalDateTime(),
                    bruker = Foedselsnummer.of(getString("fnr")),
                    behandlingId = getObject("behandling_id")?.let { it as UUID }
                )
            }
        }
    }

    private fun antallSoesken(soesken: String): Int {
        val soeskenList: List<String> = objectMapper.readValue(soesken)
        return soeskenList.size
    }
}