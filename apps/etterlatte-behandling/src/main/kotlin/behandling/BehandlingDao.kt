package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.database.singleOrNull
import no.nav.etterlatte.database.toList
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.toJson
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.*

class BehandlingDao(private val connection: () -> Connection) {

    fun hentBehandling(id: UUID): Behandling? {
        val stmt =
            connection().prepareStatement(
                "SELECT id, sak_id, behandling_opprettet, sist_endret, " +
                        "soekand_mottatt_dato, innsender, soeker, gjenlevende, avdoed, soesken, " +
                        "gyldighetssproving, status  FROM behandling where id = ?"
            )
        stmt.setObject(1, id)

        return stmt.executeQuery().singleOrNull { asBehandling(this) }
    }

    fun alleBehandlinger(): List<Behandling> {
        val stmt =
            connection().prepareStatement(
                "SELECT id, sak_id, behandling_opprettet, sist_endret, " +
                        "soekand_mottatt_dato, innsender, soeker, gjenlevende, avdoed, soesken, " +
                        "gyldighetssproving, status  FROM behandling"
            )
        return stmt.executeQuery().toList { asBehandling(this) }
    }

    fun alleBehandingerISak(sakid: Long): List<Behandling> {
        val stmt =
            connection().prepareStatement(
                "SELECT id, sak_id, behandling_opprettet, sist_endret, " +
                        "soekand_mottatt_dato, innsender, soeker, gjenlevende, avdoed, soesken, " +
                        "gyldighetssproving, status  FROM behandling where id = ?"
            )
        stmt.setLong(1, sakid)
        return stmt.executeQuery().toList { asBehandling(this) }
    }

    fun asBehandling(rs: ResultSet) = Behandling(
        id = rs.getObject("id") as UUID,
        sak = rs.getLong("sak_id"),
        behandlingOpprettet = rs.getTimestamp("behandling_opprettet").toLocalDateTime(),
        sistEndret = rs.getTimestamp("sist_endret").toLocalDateTime(),
        soeknadMottattDato = rs.getTimestamp("soekand_mottatt_dato").toLocalDateTime(),
        innsender = rs.getString("innsender"),
        soeker = rs.getString("soeker"),
        gjenlevende = rs.getString("gjenlevende")?.let { objectMapper.readValue(it) },
        avdoed = rs.getString("avdoed")?.let { objectMapper.readValue(it) },
        soesken = rs.getString("soesken")?.let { objectMapper.readValue(it) },
        gyldighetsproeving = rs.getString("gyldighetssproving")?.let { objectMapper.readValue(it) },
        status = rs.getString("status")?.let { objectMapper.readValue(it) },
    )

    fun opprett(behandling: Behandling) {
        val stmt = connection().prepareStatement("INSERT INTO behandling(id, sak_id, behandling_opprettet, sist_endret, status) VALUES(?, ?, ?, ?, ?)")
        stmt.setObject(1, behandling.id)
        stmt.setLong(2, behandling.sak)
        stmt.setTimestamp(3, Timestamp.valueOf(behandling.behandlingOpprettet))
        stmt.setTimestamp(4, Timestamp.valueOf(behandling.sistEndret))
        stmt.setString(5, behandling.status?.name)
        stmt.executeUpdate()
    }

    fun lagrePersongalleriOgMottattdato(behandling: Behandling) {
        val stmt = connection().prepareStatement("UPDATE behandling SET soekand_mottatt_dato = ?, sist_endret = ?, " +
                "innsender = ?, soeker = ?, gjenlevende = ?, avdoed = ?, soesken = ? WHERE id = ?")
        stmt.setTimestamp(1, Timestamp.valueOf(behandling.soeknadMottattDato))
        stmt.setTimestamp(2, Timestamp.valueOf(behandling.sistEndret))
        stmt.setString(3, behandling.innsender)
        stmt.setString(4, behandling.soeker)
        stmt.setString(5, behandling.gjenlevende?.toJson())
        stmt.setString(6, behandling.avdoed?.toJson())
        stmt.setString(7, behandling.soesken?.toJson())
        stmt.setObject(8, behandling.id)
    }

    fun lagreGyldighetsproving(behandling: Behandling) {
        val stmt = connection().prepareStatement("UPDATE behandling SET gyldighetssproving = ? WHERE id = ?")
        stmt.setObject(1, objectMapper.writeValueAsString(behandling.gyldighetsproeving))
        stmt.setObject(2, behandling.id)
        require(stmt.executeUpdate() == 1)
    }

    fun slettBehandlingerISak(id: Long) {
        val statement = connection().prepareStatement("DELETE from behandling where sak_id = ?")
        statement.setLong(1, id)
        statement.executeUpdate()
    }

    fun avbrytBehandling(behandling: Behandling) {
        val stmt = connection().prepareStatement("UPDATE behandling SET status = ? WHERE id = ?")
        stmt.setString(1, BehandlingStatus.AVBRUTT.name)
        stmt.setObject(2, behandling.id)
        require(stmt.executeUpdate() == 1)
    }
}

val objectMapper =
    jacksonObjectMapper().registerModule(JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

