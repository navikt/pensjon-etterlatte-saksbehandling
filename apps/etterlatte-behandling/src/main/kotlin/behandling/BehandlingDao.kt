package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.database.singleOrNull
import no.nav.etterlatte.database.toList
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import no.nav.etterlatte.libs.common.toJson
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.ZoneId
import java.util.*

class BehandlingDao(private val connection: () -> Connection) {

    fun hentBehandling(id: UUID): Behandling? {
        val stmt =
            connection().prepareStatement(
                "SELECT id, sak_id, behandling_opprettet, sist_endret, " +
                        "soekand_mottatt_dato, innsender, soeker, gjenlevende, avdoed, soesken, " +
                        "gyldighetssproving, status, oppgave_status FROM behandling where id = ?"
            )
        stmt.setObject(1, id)

        return stmt.executeQuery().singleOrNull { asBehandling(this) }
    }

    fun alleBehandlinger(): List<Behandling> {
        val stmt =
            connection().prepareStatement(
                "SELECT id, sak_id, behandling_opprettet, sist_endret, " +
                        "soekand_mottatt_dato, innsender, soeker, gjenlevende, avdoed, soesken, " +
                        "gyldighetssproving, status, oppgave_status  FROM behandling"
            )
        return stmt.executeQuery().toList { asBehandling(this) }
    }

    fun alleBehandingerISak(sakid: Long): List<Behandling> {
        val stmt =
            connection().prepareStatement(
                "SELECT id, sak_id, behandling_opprettet, sist_endret, " +
                        "soekand_mottatt_dato, innsender, soeker, gjenlevende, avdoed, soesken, " +
                        "gyldighetssproving, status, oppgave_status  FROM behandling where sak_id = ?"
            )
        stmt.setLong(1, sakid)
        return stmt.executeQuery().toList { asBehandling(this) }
    }

    fun asBehandling(rs: ResultSet) = Behandling(
        id = rs.getObject("id") as UUID,
        sak = rs.getLong("sak_id"),
        behandlingOpprettet = rs.getTimestamp("behandling_opprettet").toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
        sistEndret = rs.getTimestamp("sist_endret").toLocalDateTime(),
        soeknadMottattDato = rs.getTimestamp("soekand_mottatt_dato")?.toLocalDateTime(),
        innsender = rs.getString("innsender"),
        soeker = rs.getString("soeker"),
        gjenlevende = rs.getString("gjenlevende")?.let { objectMapper.readValue<List<String>?>(it)?.toList() },
        avdoed = rs.getString("avdoed")?.let { objectMapper.readValue<List<String>?>(it)?.toList() },
        soesken = rs.getString("soesken")?.let { objectMapper.readValue<List<String>?>(it)?.toList() },
        gyldighetsproeving = rs.getString("gyldighetssproving")?.let { objectMapper.readValue(it) },
        status = rs.getString("status")?.let { BehandlingStatus.valueOf(it) },
        oppgaveStatus = rs.getString("oppgave_status")?.let { OppgaveStatus.valueOf(it) },
    )

    fun opprett(behandling: Behandling) {
        val stmt =
            connection().prepareStatement("INSERT INTO behandling(id, sak_id, behandling_opprettet, sist_endret, status, oppgave_status) VALUES(?, ?, ?, ?, ?, ?)")
        stmt.setObject(1, behandling.id)
        stmt.setLong(2, behandling.sak)
        stmt.setTimestamp(3, Timestamp.from(behandling.behandlingOpprettet.atZone(ZoneId.systemDefault()).toInstant()))
        stmt.setTimestamp(4, Timestamp.valueOf(behandling.sistEndret))
        stmt.setString(5, behandling.status?.name)
        stmt.setString(6, behandling.oppgaveStatus?.name)
        stmt.executeUpdate()
    }

    fun lagrePersongalleriOgMottattdato(behandling: Behandling) {
        val stmt = connection().prepareStatement(
            "UPDATE behandling SET soekand_mottatt_dato = ?, sist_endret = ?, " +
                    "innsender = ?, soeker = ?, gjenlevende = ?, avdoed = ?, soesken = ? WHERE id = ?"
        )
        stmt.setTimestamp(1, Timestamp.valueOf(behandling.soeknadMottattDato))
        stmt.setTimestamp(2, Timestamp.valueOf(behandling.sistEndret))
        stmt.setString(3, behandling.innsender)
        stmt.setString(4, behandling.soeker)
        stmt.setString(5, behandling.gjenlevende?.toJson())
        stmt.setString(6, behandling.avdoed?.toJson())
        stmt.setString(7, behandling.soesken?.toJson())
        stmt.setObject(8, behandling.id)
        require(stmt.executeUpdate() == 1)
    }

    fun lagreGyldighetsproving(behandling: Behandling) {
        val stmt = connection().prepareStatement("UPDATE behandling SET gyldighetssproving = ?, status = ?, oppgave_status = ? WHERE id = ?")
        stmt.setObject(1, objectMapper.writeValueAsString(behandling.gyldighetsproeving))
        stmt.setString(2, behandling.status?.name)
        stmt.setString(3, behandling.oppgaveStatus?.name)
        stmt.setObject(4, behandling.id)
        require(stmt.executeUpdate() == 1)
    }

    fun slettBehandlingerISak(id: Long) {
        val statement = connection().prepareStatement("DELETE from behandling where sak_id = ?")
        statement.setLong(1, id)
        statement.executeUpdate()
    }

    fun lagreStatus(lagretBehandling: Behandling) {
        lagreStatus(lagretBehandling.id, lagretBehandling.status)
    }

    private fun lagreStatus(behandling: UUID, status: BehandlingStatus?){
        val stmt = connection().prepareStatement("UPDATE behandling SET status = ? WHERE id = ?")
        stmt.setString(1, status?.name)
        stmt.setObject(2, behandling)
        require(stmt.executeUpdate() == 1)
    }

    fun lagreOppgaveStatus(behandling: Behandling) {
        val stmt = connection().prepareStatement("UPDATE behandling SET oppgave_status = ? WHERE id = ?")
        stmt.setString(1, behandling.oppgaveStatus?.name)
        stmt.setObject(2, behandling.id)
        require(stmt.executeUpdate() == 1)
    }
}

val objectMapper =
    jacksonObjectMapper().registerModule(JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

