package no.nav.etterlatte.sak

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.database.single
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Types

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

    fun opprettSak(fnr: String, type: SakType, enhet: String? = null): Sak {
        val statement =
            connection().prepareStatement(
                "INSERT INTO sak(sakType, fnr, enhet) VALUES(?, ?, ?) RETURNING id, sakType, fnr, enhet"
            )
        statement.setString(1, type.name)
        statement.setString(2, fnr)
        enhet?.let { statement.setString(3, it) } ?: statement.setNull(3, Types.VARCHAR)
        return requireNotNull(
            statement.executeQuery().singleOrNull { this.toSak() }
        )
    }

    // TODO: høre med Lars Erik om det kan returneres en liste av saker her, mtp at hver person kun kan ha
    // én tilhørende sak. Hvs så, skriv om til singleOrNull
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

    fun markerSakerMedSkjerming(sakIder: List<Long>, skjermet: Boolean) {
        with(connection()) {
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

    fun enAvSakeneHarAdresseBeskyttelse(sakIder: List<Long>): Boolean {
        with(connection()) {
            val statement = prepareStatement(
                """
                 SELECT count(*) as gradert 
                 from sak
                 where id = any(?)
                 AND adressebeskyttelse is NOT NULL AND adressebeskyttelse IN (?, ?, ?)
                """.trimIndent()
            )
            statement.setArray(1, createArrayOf("bigint", sakIder.toTypedArray()))
            statement.setString(2, AdressebeskyttelseGradering.STRENGT_FORTROLIG.toString())
            statement.setString(3, AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND.toString())
            statement.setString(4, AdressebeskyttelseGradering.FORTROLIG.toString())
            val resultSet = statement.executeQuery()
            return resultSet.single {
                getInt(1)
            } > 0
        }
    }

    private fun ResultSet.toSak() = Sak(
        sakType = enumValueOf(getString("sakType")),
        ident = getString("fnr"),
        id = getLong("id"),
        enhet = getString("enhet").takeIf { !wasNull() }
    )
}