package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.database.singleOrNull
import no.nav.etterlatte.database.toList
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning

import java.sql.Connection
import java.util.*

class GrunnlagDao(private val connection: () -> Connection) {

    fun hent(saksid: Long): Grunnlag? {
        val stmt =
            connection().prepareStatement("SELECT id, sak_id FROM behandling where id = ?")
        stmt.setObject(1, saksid)
        return stmt.executeQuery().singleOrNull {
            Grunnlag(
                getObject(1) as UUID,
                getLong(2),
                emptyList()
            )
        }
    }

    fun alle(): List<Grunnlag> {
        val stmt =
            connection().prepareStatement("SELECT id, sak_id, FROM grunnlag")
        return stmt.executeQuery().toList {
            Grunnlag(
                getObject(1) as UUID,
                getLong(2),
                emptyList()
            )
        }
    }

    fun alleISak(sakid: Long): List<Grunnlag> {
        val stmt =
            connection().prepareStatement("SELECT id, sak_id FROM grunnlag WHERE sak_id = ?")
        stmt.setLong(1, sakid)
        return stmt.executeQuery().toList {
            Grunnlag(
                getObject(1) as UUID,
                getLong(2),
                emptyList()
            )
        }
    }

    fun opprett(grunnlag: Grunnlag) {
        val stmt = connection().prepareStatement("INSERT INTO behandling(id, sak_id) VALUES(?, ?)")
        stmt.setObject(1, grunnlag.id)
        stmt.setLong(2, grunnlag.saksId)
        stmt.executeUpdate()
    }



    fun hentBehandlingerMedSakId(id: Long): List<Grunnlag> {
        val stmt =
            connection().prepareStatement("SELECT id, sak_id from behandling where sak_id = ?")
        stmt.setLong(1, id)
        return stmt.executeQuery().toList {
            Grunnlag(
                getObject(1) as UUID,
                getLong(2),
                emptyList()
            )
        }
    }

    /*
    fun avbrytBehandling(grunnlag: Grunnlag) {
        val stmt = connection().prepareStatement("UPDATE behandling SET avbrutt = ? WHERE id = ?")
        stmt.setBoolean(1, true)
        stmt.setObject(2, grunnlag.id)
        require(stmt.executeUpdate() == 1)
    }

     */


}

val objectMapper =
    jacksonObjectMapper().registerModule(JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

fun ObjectNode?.serialize() = this?.let { objectMapper.writeValueAsString(it) }
fun String?.deSerialize() = this?.let { objectMapper.readValue(this, ObjectNode::class.java) }


