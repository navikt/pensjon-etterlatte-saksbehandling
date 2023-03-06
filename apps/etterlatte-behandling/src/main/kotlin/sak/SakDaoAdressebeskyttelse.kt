package no.nav.etterlatte.sak

import no.nav.etterlatte.libs.common.pdlhendelse.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.database.singleOrNull
import javax.sql.DataSource

class SakDaoAdressebeskyttelse(private val datasource: DataSource) {

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