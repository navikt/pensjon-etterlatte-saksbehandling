package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.database.singleOrNull
import no.nav.etterlatte.database.toList
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import java.sql.Connection
import java.time.Instant
import java.util.*

class OpplysningDao(private val connection: () -> Connection) {
    private fun objectToKilde(o: ObjectNode): Behandlingsopplysning.Kilde {
        return o.get("type").textValue().let {
            when (it) {
                "saksbehandler" -> Behandlingsopplysning.Saksbehandler(o.get("ident").textValue())
                "privatperson" -> Behandlingsopplysning.Privatperson(
                    o.get("fnr").textValue(),
                    Instant.parse(o.get("mottatDato").textValue())
                )
                else -> throw IllegalArgumentException()
            }
        }
    }

    fun hent(id: UUID): Behandlingsopplysning? {
        return connection().prepareStatement("SELECT o.id, o.kilde,  o.type, o.meta, o.data  FROM opplysning where id = ?")
            .apply {
                setObject(1, id)
            }.executeQuery().singleOrNull {
            Behandlingsopplysning(
                getObject(1) as UUID,
                objectToKilde(getString(2).deSerialize()!!),
                getString(3),
                getString(4).deSerialize()!!,
                getString(5).deSerialize()!!,

                )
        }
    }

    fun finnOpplysningerIBehandling(behandling: UUID): List<Behandlingsopplysning> {
        return connection().prepareStatement("SELECT o.id, o.kilde,  o.type, o.meta, o.data  FROM opplysning o inner join opplysning_i_behandling oib on o.id = oib.opplysning_id and oib.behandling_id = ?")
            .apply {
                setObject(1, behandling)
            }.executeQuery().toList {
            Behandlingsopplysning(
                getObject(1) as UUID,
                objectToKilde(getString(2).deSerialize()!!),
                getString(3),
                getString(4).deSerialize()!!,
                getString(5).deSerialize()!!,
            )
        }

    }

    fun nyOpplysning(behandlingsopplysning: Behandlingsopplysning) {
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
}
