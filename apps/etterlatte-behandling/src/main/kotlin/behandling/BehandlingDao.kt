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
import java.sql.PreparedStatement
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


        return stmt.executeQuery().singleOrNull {
            Behandling(
                id = getObject(1) as UUID,
                sak = getLong(2),
                behandlingOpprettet = getTimestamp(3).toLocalDateTime(),
                sistEndret = getTimestamp(4).toLocalDateTime(),
                soeknadMottattDato = getDate(5).toLocalDate(), //todo endres til datetime?
                innsender = getString(6),
                soeker = getString(7),
                gjenlevende = getString(8)?.let { objectMapper.readValue(it) },
                avdoed = getString(9)?.let { objectMapper.readValue(it) },
                soesken = getString(10)?.let { objectMapper.readValue(it) },
                gyldighetsproeving = getString(11)?.let { objectMapper.readValue(it) },
                status = getString(12)?.let { BehandlingStatus.valueOf(it) },
            )
        }
    }

    fun asBehandling(rs: ResultSet) =  Behandling(
        id = rs.getObject(1) as UUID,
        sak = rs.getLong(2),
        behandlingOpprettet = rs.getTimestamp(3).toLocalDateTime(),
        sistEndret = rs.getTimestamp(4).toLocalDateTime(),
        soeknadMottattDato = rs.getDate(5).toLocalDate(), //todo endres til datetime?
        innsender = rs.getString(6),
        soeker = rs.getString(7),
        gjenlevende = rs.getString(8)?.let { objectMapper.readValue(it) },
        avdoed = rs.getString(9)?.let { objectMapper.readValue(it) },
        soesken = rs.getString(10)?.let { objectMapper.readValue(it) },
        gyldighetsproeving = rs.getString(11)?.let { objectMapper.readValue(it) },
        status = rs.getString(12)?.let { objectMapper.readValue(it) },
    )

    fun alleBehandlinger(): List<Behandling> {
        val stmt =
            connection().prepareStatement(
                "SELECT id, sak_id, behandlingOpprettet, sistEndret, " +
                        "soeknadMottattDato, innsender, soeker, gjenlevende, avdoed, soesken, " +
                        "gyldighetsproeving, status  FROM behandling"
            )
        return listeResultat(stmt)
    }

    fun alleBehandingerISak(sakid: Long): List<Behandling> {
        val stmt =
            connection().prepareStatement(
                "SELECT id, sak_id, behandlingOpprettet, sistEndret, " +
                        "soeknadMottattDato, innsender, soeker, gjenlevende, avdoed, soesken, " +
                        "gyldighetsproeving, status  FROM behandling where id = ?"
            )
        stmt.setLong(1, sakid)
        return listeResultat(stmt)
    }

    fun listeResultat(stmt: PreparedStatement): List<Behandling> {
        return stmt.executeQuery().toList {
            Behandling(
                id = getObject(1) as UUID,
                sak = getLong(2),
                behandlingOpprettet = getTimestamp(3).toLocalDateTime(),
                sistEndret = getTimestamp(4).toLocalDateTime(),
                soeknadMottattDato = getDate(5).toLocalDate(),
                innsender = getString(6),
                soeker = getString(7),
                gjenlevende = getString(8)?.let { objectMapper.readValue(it) },
                avdoed = getString(9)?.let { objectMapper.readValue(it) },
                soesken = getString(10)?.let { objectMapper.readValue(it) },
                gyldighetsproeving = getString(11)?.let { objectMapper.readValue(it) },
                status = getString(12)?.let { objectMapper.readValue(it) },
            )
        }
    }

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
        val stmt = connection().prepareStatement("UPDATE behandling SET avbrutt = ? WHERE id = ?")
        stmt.setBoolean(1, true)
        stmt.setObject(2, behandling.id)
        require(stmt.executeUpdate() == 1)
    }
}

val objectMapper =
    jacksonObjectMapper().registerModule(JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

fun ObjectNode?.serialize() = this?.let { objectMapper.writeValueAsString(it) }
fun String?.deSerialize() = this?.let { objectMapper.readValue(this, ObjectNode::class.java) }


