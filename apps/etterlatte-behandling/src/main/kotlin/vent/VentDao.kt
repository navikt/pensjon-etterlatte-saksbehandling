package no.nav.etterlatte.vent

import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.toList
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.UUID

class VentDao(private val connection: () -> Connection) {
    fun hentVentaFerdig(dato: Tidspunkt): List<Vent> =
        with(connection()) {
            val statement =
                prepareStatement(
                    """
                    select $IDKOLONNE as $IDKOLONNE, 
                        $OPPGAVE_ID as $OPPGAVE_ID, 
                        $BEHANDLING_ID as $BEHANDLING_ID, 
                        $VENTETYPE as $VENTETYPE, 
                        $PAA_VENT_TIL as $PAA_VENT_TIL
                    from $TABELLNAVN
                    where $HAANDTERT = false
                    and $PAA_VENT_TIL <= ? 
                    """.trimIndent(),
                )
            statement.setTidspunkt(1, dato)
            statement.executeQuery().toList { asVentDao() }
        }

    private fun ResultSet.asVentDao() =
        Vent(
            id = getUUID(IDKOLONNE),
            oppgaveId = getUUID(OPPGAVE_ID),
            behandlingId = getUUID(BEHANDLING_ID),
            ventetype = Ventetype.valueOf(getString(VENTETYPE)),
            paaVentTil = getTidspunkt(PAA_VENT_TIL),
        )

    fun lagreHaandtert(id: UUID) =
        with(connection()) {
            val statement = prepareStatement("update $TABELLNAVN set $HAANDTERT = true where $IDKOLONNE = ?::UUID")
            statement.setString(1, id.toString())
            statement.executeUpdate()
        }

    fun settPaaVent(vent: Vent) {
        with(connection()) {
            val statement =
                prepareStatement(
                    """
                    insert into $TABELLNAVN ($IDKOLONNE, $OPPGAVE_ID, $BEHANDLING_ID, $VENTETYPE, $PAA_VENT_TIL)
                    values(?::UUID, ?::UUID, ?::UUID, ?, ?)
                    """.trimIndent(),
                )
            statement.setUUID(1, vent.id)
            statement.setUUID(2, vent.oppgaveId)
            statement.setUUID(3, vent.behandlingId)
            statement.setString(4, vent.ventetype.name)
            statement.setTidspunkt(5, vent.paaVentTil)
            statement.executeUpdate()
        }
    }

    companion object {
        const val TABELLNAVN = "vent"
        const val IDKOLONNE = "id"
        const val OPPGAVE_ID = "oppgaveId"
        const val BEHANDLING_ID = "behandlingId"
        const val VENTETYPE = "ventetype"
        const val HAANDTERT = "haandtert"
        const val PAA_VENT_TIL = "paa_vent_til"
    }
}

fun PreparedStatement.setUUID(
    index: Int,
    uuid: UUID,
) = setString(index, uuid.toString())
