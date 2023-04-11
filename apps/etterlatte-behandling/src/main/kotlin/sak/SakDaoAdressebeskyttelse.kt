package no.nav.etterlatte.sak

import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.database.single
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import javax.sql.DataSource

class SakDaoAdressebeskyttelse(private val datasource: DataSource) {

    fun enAvSakeneHarAdresseBeskyttelse(sakIder: List<Long>): Boolean {
        datasource.connection.use {
            val statement = it.prepareStatement(
                """
                 SELECT count(*) as gradert 
                 from sak
                 where id = any(?)
                 AND adressebeskyttelse is NOT NULL AND adressebeskyttelse IN (?, ?, ?)
                """.trimIndent()
            )
            statement.setArray(1, it.createArrayOf("bigint", sakIder.toTypedArray()))
            statement.setString(
                2,
                AdressebeskyttelseGradering.STRENGT_FORTROLIG.toString()
            )
            statement.setString(
                3,
                AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND.toString()
            )
            statement.setString(
                4,
                AdressebeskyttelseGradering.FORTROLIG.toString()
            )
            val resultSet = statement.executeQuery()
            return resultSet.single {
                getInt(1)
            } > 0
        }
    }
    fun finnSaker(fnr: String): List<Sak> {
        datasource.connection.use {
            val statement = it.prepareStatement("SELECT id, sakType, fnr from sak where fnr = ?")
            statement.setString(1, fnr)
            return statement.executeQuery().toList {
                Sak(
                    sakType = enumValueOf(getString(2)),
                    ident = getString(3),
                    id = getLong(1)
                )
            }
        }
    }

    fun oppdaterAdresseBeskyttelse(id: Long, adressebeskyttelseGradering: AdressebeskyttelseGradering): Int {
        datasource.connection.use {
            val statement = it.prepareStatement("UPDATE sak SET adressebeskyttelse = ? where id = ?")
            statement.setString(1, adressebeskyttelseGradering.toString())
            statement.setLong(2, id)
            return statement.executeUpdate()
        }
    }

    fun sjekkOmBehandlingHarAdressebeskyttelse(behandlingId: String): AdressebeskyttelseGradering? {
        datasource.connection.use {
            val statement = it.prepareStatement(
                "SELECT adressebeskyttelse FROM behandling b INNER JOIN sak s ON b.sak_id = s.id WHERE b.id = ?::uuid"
            )
            statement.setString(1, behandlingId)
            return statement.executeQuery().singleOrNull {
                val adressebeskyttelse = getString(1)
                adressebeskyttelse?.let { AdressebeskyttelseGradering.valueOf(it) }
            }
        }
    }
}