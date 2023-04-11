package no.nav.etterlatte.sak

import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import javax.sql.DataSource

class SakDaoAdressebeskyttelse(private val datasource: DataSource) {

    fun finnSakerMedGradering(fnr: String): List<SakMedGradering> {
        datasource.connection.use {
            val statement = it.prepareStatement("SELECT id, adressebeskyttelse from sak where fnr = ?")
            statement.setString(1, fnr)
            return statement.executeQuery().toList {
                SakMedGradering(
                    id = getLong(1),
                    adressebeskyttelseGradering = getString(2)?.let { AdressebeskyttelseGradering.valueOf(it) }
                )
            }
        }
    }

    fun hentSakMedGradering(id: Long): SakMedGradering? {
        datasource.connection.use {
            val statement = it.prepareStatement("SELECT id, adressebeskyttelse from sak where id = ?")
            statement.setLong(1, id)
            return statement.executeQuery().singleOrNull {
                SakMedGradering(
                    id = getLong(1),
                    adressebeskyttelseGradering = getString(2)?.let { AdressebeskyttelseGradering.valueOf(it) }
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