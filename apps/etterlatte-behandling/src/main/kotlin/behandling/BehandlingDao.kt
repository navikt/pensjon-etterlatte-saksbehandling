package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.database.singleOrNull
import no.nav.etterlatte.database.toList
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import java.sql.Connection
import java.sql.ResultSet
import java.util.*

class BehandlingDao(private val connection: () -> Connection) {

    fun hentBehandling(id: UUID): Behandling? {
        val stmt =
            connection().prepareStatement(
                "SELECT id, sak_id, behandlingOpprettet, sistEndret, " +
                        "soeknadMottattDato, innsender, soeker, gjenlevende, avdoed, soesken, " +
                        "gyldighetsproeving, status  FROM behandling where id = ?"
            )
        stmt.setObject(1, id)

        return stmt.executeQuery().singleOrNull { asBehandling(this) }
    }

    fun alleBehandlinger(): List<Behandling> {
        val stmt =
            connection().prepareStatement(
                "SELECT id, sak_id, behandlingOpprettet, sistEndret, " +
                        "soeknadMottattDato, innsender, soeker, gjenlevende, avdoed, soesken, " +
                        "gyldighetsproeving, status  FROM behandling"
            )
        return stmt.executeQuery().toList { asBehandling(this) }
    }

    fun alleBehandingerISak(sakid: Long): List<Behandling> {
        val stmt =
            connection().prepareStatement(
                "SELECT id, sak_id, behandlingOpprettet, sistEndret, " +
                        "soeknadMottattDato, innsender, soeker, gjenlevende, avdoed, soesken, " +
                        "gyldighetsproeving, status  FROM behandling where id = ?"
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
        val stmt = connection().prepareStatement("INSERT INTO behandling(id, sak_id) VALUES(?, ?)")
        stmt.setObject(1, behandling.id)
        stmt.setLong(2, behandling.sak)
        stmt.executeUpdate()
    }

    fun lagreGyldighetsproving(behandling: Behandling) {
        val stmt = connection().prepareStatement("UPDATE behandling SET gyldighetsproving = ? WHERE id = ?")
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

fun ObjectNode?.serialize() = this?.let { objectMapper.writeValueAsString(it) }
fun String?.deSerialize() = this?.let { objectMapper.readValue(this, ObjectNode::class.java) }


