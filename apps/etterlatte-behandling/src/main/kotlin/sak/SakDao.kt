package no.nav.etterlatte.sak

import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.Connection
import java.sql.ResultSet

class SakDao(private val connection: () -> Connection) {
    fun hentSaker(): List<Sak> {
        val statement = connection().prepareStatement("SELECT id, sakType, fnr, enhet from sak")
        return statement.executeQuery().toList { this.toSak() }
    }

    fun hentSak(id: Long): Sak? {
        val statement = connection().prepareStatement("SELECT id, sakType, fnr, enhet from sak where id = ?")
        statement.setLong(1, id)
        return statement.executeQuery().singleOrNull { this.toSak() }
    }

    fun opprettSak(fnr: String, type: SakType, enhet: String): Sak {
        val statement =
            connection().prepareStatement(
                "INSERT INTO sak(sakType, fnr, enhet) VALUES(?, ?, ?) RETURNING id, sakType, fnr, enhet"
            )
        statement.setString(1, type.name)
        statement.setString(2, fnr)
        statement.setString(3, enhet)
        return requireNotNull(
            statement.executeQuery().singleOrNull { this.toSak() }
        )
    }

    fun oppdaterEnheterPaaSaker(saker: List<GrunnlagsendringshendelseService.SakMedEnhet>) {
        with(connection()) {
            val statement = prepareStatement(
                """
                UPDATE sak 
                set enhet = ? 
                where id = ?
                """.trimIndent()
            )
            saker.forEach {
                statement.setString(1, it.enhet)
                statement.setLong(2, it.id)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    fun finnSaker(fnr: String): List<Sak> {
        val statement = connection().prepareStatement("SELECT id, sakType, fnr, enhet from sak where fnr = ?")
        statement.setString(1, fnr)
        return statement.executeQuery().toList { this.toSak() }
    }

    fun slettSak(id: Long) {
        val statement = connection().prepareStatement("DELETE from sak where id = ?")
        statement.setLong(1, id)
        statement.executeUpdate()
    }

    fun markerSakerMedSkjerming(sakIder: List<Long>, skjermet: Boolean): Int {
        return with(connection()) {
            val statement = prepareStatement(
                """
                UPDATE sak 
                set erSkjermet = ? 
                where id = any(?)
                """.trimIndent()
            )
            statement.setBoolean(1, skjermet)
            statement.setArray(2, createArrayOf("bigint", sakIder.toTypedArray()))
            statement.executeUpdate()
        }
    }

    private fun ResultSet.toSak() = Sak(
        sakType = enumValueOf(getString("sakType")),
        ident = getString("fnr"),
        id = getLong("id"),
        enhet = getString("enhet")
    )
}