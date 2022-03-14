package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.database.singleOrNull
import no.nav.etterlatte.database.toList
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning

import java.sql.Connection
import java.util.*

class BehandlingDao(private val connection: () -> Connection) {

    fun hent(id: UUID): Behandling? {
        val stmt =
            connection().prepareStatement("SELECT id, sak_id, vilkaarsproving, beregning, fastsatt, avbrutt FROM behandling where id = ?")
        stmt.setObject(1, id)
        return stmt.executeQuery().singleOrNull {
            Behandling(
                getObject(1) as UUID,
                getLong(2),
                emptyList(),
                getString(3)?.let { objectMapper.readValue(it) },
                getString(4)?.let { objectMapper.readValue(it) },
                getBoolean(5),
                getBoolean(6)
            )
        }
    }

    fun alle(): List<Behandling> {
        val stmt =
            connection().prepareStatement("SELECT id, sak_id, vilkaarsproving, beregning, fastsatt, avbrutt FROM behandling")
        return stmt.executeQuery().toList {
            Behandling(
                getObject(1) as UUID,
                getLong(2),
                emptyList<Behandlingsopplysning<ObjectNode>>(),
                getString(3)?.let { objectMapper.readValue(it) },
                getString(5)?.let { objectMapper.readValue(it) },
                getBoolean(5),
                getBoolean(6)
            )
        }
    }

    fun alleISak(sakid: Long): List<Behandling> {
        val stmt =
            connection().prepareStatement("SELECT id, sak_id, vilkaarsproving, beregning, fastsatt, avbrutt FROM behandling WHERE sak_id = ?")
        stmt.setLong(1, sakid)
        return stmt.executeQuery().toList {
            Behandling(
                getObject(1) as UUID,
                getLong(2),
                emptyList(),
                getString(3)?.let { objectMapper.readValue(it) },
                getString(4)?.let { objectMapper.readValue(it) },
                getBoolean(5),
                getBoolean(6)
            )
        }
    }

    fun opprett(behandling: Behandling) {
        val stmt = connection().prepareStatement("INSERT INTO behandling(id, sak_id) VALUES(?, ?)")
        stmt.setObject(1, behandling.id)
        stmt.setLong(2, behandling.sak)
        stmt.executeUpdate()
    }

    fun lagreBeregning(behandling: Behandling) {
        val stmt = connection().prepareStatement("UPDATE behandling SET beregning = ? WHERE id = ?")
        stmt.setObject(1, objectMapper.writeValueAsString(behandling.beregning))
        stmt.setObject(2, behandling.id)
        require(stmt.executeUpdate() == 1)
    }

    fun lagreVilkarsproving(behandling: Behandling) {
        val stmt = connection().prepareStatement("UPDATE behandling SET vilkaarsproving = ? WHERE id = ?")
        stmt.setObject(1, objectMapper.writeValueAsString(behandling.vilkårsprøving))
        stmt.setObject(2, behandling.id)
        require(stmt.executeUpdate() == 1)
    }

    fun lagreFastsett(behandling: Behandling) {
        val stmt = connection().prepareStatement("UPDATE behandling SET fastsatt = ? WHERE id = ?")
        stmt.setBoolean(1, behandling.fastsatt)
        stmt.setObject(2, behandling.id)
        require(stmt.executeUpdate() == 1)
    }

    fun slettBehandlingerISak(id: Long) {
        val statement = connection().prepareStatement("DELETE from behandling where sak_id = ?")
        statement.setLong(1, id)
        statement.executeUpdate()
    }

    fun hentBehandlingerMedSakId(id: Long): List<Behandling> {
        val stmt =
            connection().prepareStatement("SELECT id, sak_id, vilkaarsproving, beregning, fastsatt, avbrutt from behandling where sak_id = ?")
        stmt.setLong(1, id)
        return stmt.executeQuery().toList {
            Behandling(
                getObject(1) as UUID,
                getLong(2),
                emptyList<Behandlingsopplysning<ObjectNode>>(),
                getString(3)?.let { objectMapper.readValue(it) },
                getString(4)?.let { objectMapper.readValue(it) },
                getBoolean(5),
                getBoolean(6)
            )
        }
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


