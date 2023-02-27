package no.nav.etterlatte.sak

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdlhendelse.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.Connection

class SakDao(private val connection: () -> Connection) {
    fun hentSaker(): List<Sak> {
        val statement = connection().prepareStatement("SELECT id, sakType, fnr from sak")
        return statement.executeQuery().toList {
            Sak(
                sakType = enumValueOf(getString(2)),
                ident = getString(3),
                id = getLong(1)
            )
        }
    }

    fun hentSak(id: Long): Sak? {
        val statement = connection().prepareStatement("SELECT id, sakType, fnr from sak where id = ?")
        statement.setLong(1, id)
        return statement.executeQuery().singleOrNull {
            Sak(
                sakType = enumValueOf(getString(2)),
                ident = getString(3),
                id = getLong(1)
            )
        }
    }

    fun opprettSak(fnr: String, type: SakType): Sak {
        val statement =
            connection().prepareStatement("INSERT INTO sak(sakType, fnr) VALUES(?, ?) RETURNING id, sakType, fnr")
        statement.setString(1, type.name)
        statement.setString(2, fnr)
        return requireNotNull(
            statement.executeQuery().singleOrNull {
                Sak(
                    sakType = enumValueOf(getString(2)),
                    ident = getString(3),
                    id = getLong(1)
                )
            }
        )
    }

    // TODO: høre med Lars Erik om det kan returneres en liste av saker her, mtp at hver person kun kan ha
    // én tilhørende sak. Hvs så, skriv om til singleOrNull
    fun finnSaker(fnr: String): List<Sak> {
        val statement = connection().prepareStatement("SELECT id, sakType, fnr from sak where fnr = ?")
        statement.setString(1, fnr)
        return statement.executeQuery().toList {
            Sak(
                sakType = enumValueOf(getString(2)),
                ident = getString(3),
                id = getLong(1)
            )
        }
    }

    fun slettSak(id: Long) {
        val statement = connection().prepareStatement("DELETE from sak where id = ?")
        statement.setLong(1, id)
        statement.executeUpdate()
    }

    fun setAdresseBeskyttelse(id: Long, adressebeskyttelseGradering: AdressebeskyttelseGradering): Int {
        val statement = connection().prepareStatement("UPDATE sak SET adressebeskyttelse = ? where id = ?")
        statement.setString(1, adressebeskyttelseGradering.toString())
        statement.setLong(2, id)
        return statement.executeUpdate()
    }
}