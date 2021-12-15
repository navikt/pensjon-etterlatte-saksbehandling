package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.database.singleOrNull
import no.nav.etterlatte.database.toList

import java.sql.Connection
import java.util.*

class BehandlingDao(private val connection: ()->Connection) {

    fun hent(id: UUID): Behandling? {
        val stmt = connection().prepareStatement("SELECT id, sak_id, vilkaarsproving, beregning FROM behandling where id = ?")
        stmt.setObject(1, id)
        return stmt.executeQuery().singleOrNull {
            Behandling(
                getObject(1) as UUID,
                getLong(2).toString(),
                emptyList(),
                getString(3)?.let { objectMapper.readValue(it) },
                getString(4)?.let { objectMapper.readValue(it) }
            ) }
    }

    fun alle(): List<Behandling> {
        val stmt = connection().prepareStatement("SELECT id, sak_id, vilkaarsproving, beregning FROM behandling")
        return stmt.executeQuery().toList {
            Behandling(
                getObject(1) as UUID,
                getLong(2).toString(),
                emptyList<Opplysning>(),
                objectMapper.readValue(getString(3)),
                objectMapper.readValue(getString(4))
            ) }
    }
    fun opprett(behandling: Behandling){
        val stmt = connection().prepareStatement("INSERT INTO behandling(id, sak_id) VALUES(?, ?)")
        stmt.setObject(1, behandling.id)
        stmt.setLong(2, behandling.sak.toLong())
        stmt.executeUpdate()
    }
    fun lagreBeregning(behandling: Behandling){
        val stmt = connection().prepareStatement("UPDATE behandling SET beregning = ? WHERE id = ?")
        stmt.setObject(1, objectMapper.writeValueAsString(behandling.beregning))
        stmt.setObject(2, behandling.id)
        require(stmt.executeUpdate() == 1)
    }

    fun lagreVilkarsproving(behandling: Behandling){
        val stmt = connection().prepareStatement("UPDATE behandling SET vilkaarsproving = ? WHERE id = ?")
        stmt.setObject(1, objectMapper.writeValueAsString(behandling.vilkårsprøving))
        stmt.setObject(2, behandling.id)
        require(stmt.executeUpdate() == 1)
    }

    fun lagreFastsett(behandling: Behandling){
        val stmt = connection().prepareStatement("UPDATE behandling SET fastsatt = ? WHERE id = ?")
        stmt.setBoolean(1, behandling.fastsatt)
        stmt.setObject(2, behandling.id)
        require(stmt.executeUpdate() == 1)
    }



}

val objectMapper = jacksonObjectMapper()
fun ObjectNode?.serialize() = this?.let { objectMapper.writeValueAsString(it) }
fun String?.deSerialize() = this?.let { objectMapper.readValue(this, ObjectNode::class.java) }


