package no.nav.etterlatte.oppgave

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.sak.Sak
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import javax.sql.DataSource

enum class Rolle {
    SAKSBEHANDLER, ATTESTANT
}

data class Oppgave(
    val behandlingId: UUID,
    val behandlingStatus: BehandlingStatus,
    val sak: Sak,
    val regdato: ZonedDateTime,
    val fristDato: LocalDate,
    val behandlingsType: BehandlingType,
    val antallSoesken: Int
) {
    val oppgaveStatus: OppgaveStatus = OppgaveStatus.from(behandlingStatus)
}

class OppgaveDao(private val datasource: DataSource) {

    fun finnOppgaverForRoller(roller: List<Rolle>): List<Oppgave> {
        val aktuelleStatuser = roller.flatMap { rolle ->
            when (rolle) {
                Rolle.SAKSBEHANDLER -> BehandlingStatus.kanEndres()

                Rolle.ATTESTANT -> listOf(BehandlingStatus.FATTET_VEDTAK)
            }
        }.distinct()

        if (aktuelleStatuser.isEmpty()) return emptyList()

        datasource.connection.use {
            val stmt = it.prepareStatement(
                """
                |SELECT b.id, b.sak_id, soeknad_mottatt_dato, fnr, sakType, status, behandling_opprettet,
                |behandlingstype, soesken 
                |FROM behandling b INNER JOIN sak s ON b.sak_id = s.id 
                |WHERE status IN ${
                    aktuelleStatuser.joinToString(
                        separator = ", ",
                        prefix = "(",
                        postfix = ")"
                    ) { "'${it.name}'" }
                }
                """.trimMargin()
            )
            return stmt.executeQuery().toList {
                val mottattDato = getTimestamp("soeknad_mottatt_dato")?.toLocalDateTime()?.atZone(ZoneId.of("UTC"))
                    ?: getTimestamp("behandling_opprettet")?.toLocalDateTime()?.atZone(ZoneId.of("UTC"))
                    ?: throw IllegalStateException(
                        "Vi har en behandling som hverken har soekand mottatt dato eller behandling opprettet dato "
                    )
                Oppgave(
                    getObject("id") as UUID,
                    BehandlingStatus.valueOf(getString("status")),
                    Sak(getString("fnr"), enumValueOf(getString("sakType")), getLong("sak_id")),
                    mottattDato,
                    mottattDato.toLocalDate().plusMonths(1),
                    BehandlingType.valueOf(getString("behandlingstype")),
                    antallSoesken(getString("soesken"))
                )
            }.also {
                println("""Hentet oppgaveliste for bruker med roller $roller. Fant ${it.size} oppgaver""")
            }
        }
    }

    private fun antallSoesken(soesken: String): Int {
        val soeskenList: List<String> = objectMapper.readValue(soesken)
        return soeskenList.size
    }
}