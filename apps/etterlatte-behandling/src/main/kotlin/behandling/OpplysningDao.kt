package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.database.singleOrNull
import no.nav.etterlatte.database.toList
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import java.sql.Connection
import java.sql.ResultSet
import java.util.*

class OpplysningDao(private val connection: () -> Connection) {

    fun hent(id: UUID): Behandlingsopplysning<ObjectNode>? {
        return connection().prepareStatement("SELECT o.id, o.kilde,  o.type, o.meta, o.data  FROM opplysning o where id = ?")
            .apply {
                setObject(1, id)
            }.executeQuery().singleOrNull{ asBehandlingOpplysning() }
    }
    fun ResultSet.asBehandlingOpplysning(): Behandlingsopplysning<ObjectNode> {
        return Behandlingsopplysning(
            getObject(1) as UUID,
            objectMapper.readValue(getString(2)),
            getString(3),
            getString(4).deSerialize()!!,
            getString(5).deSerialize()!!,
        )

    }
    fun finnOpplysningerIBehandling(behandling: UUID): List<Behandlingsopplysning<ObjectNode>> {
        return connection().prepareStatement("SELECT o.id, o.kilde,  o.type, o.meta, o.data  FROM opplysning o inner join opplysning_i_behandling oib on o.id = oib.opplysning_id and oib.behandling_id = ?")
            .apply {
                setObject(1, behandling)
            }.executeQuery().toList { asBehandlingOpplysning() }

    }

    fun nyOpplysning(behandlingsopplysning: Behandlingsopplysning<ObjectNode>) {
        connection().prepareStatement("INSERT INTO opplysning(id, data, kilde, type, meta) VALUES(?, ?, ?, ?, ?)")
            .apply {
                setObject(1, behandlingsopplysning.id)
                setString(2, behandlingsopplysning.opplysning.serialize())
                setString(3, behandlingsopplysning.kilde.toJson())
                setString(4, behandlingsopplysning.opplysningType)
                setString(5, behandlingsopplysning.meta.serialize())
                require(executeUpdate() == 1)
            }
    }

    fun leggOpplysningTilBehandling(behandling: UUID, opplysning: UUID) {
        connection().prepareStatement("INSERT INTO opplysning_i_behandling(behandling_id, opplysning_id) VALUES(?, ?)")
            .apply {
                setObject(1, behandling)
                setObject(2, opplysning)
                require(executeUpdate() == 1)
            }

    }

    fun slettOpplysningerISak(id: Long){
        val statement = connection().prepareStatement("DELETE from opplysning_i_behandling where behandling_id in (select b.id from behandling b where sak_id = ?)")
        statement.setLong(1, id)
        statement.executeUpdate()
        prune()
    }

    private fun prune(){
        connection().prepareStatement("DELETE from opplysning where id not in (select opplysning_id from opplysning_i_behandling)").executeUpdate()

    }
}
