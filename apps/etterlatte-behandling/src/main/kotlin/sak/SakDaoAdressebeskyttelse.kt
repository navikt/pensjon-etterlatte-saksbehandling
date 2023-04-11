package no.nav.etterlatte.sak

import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import javax.sql.DataSource

class SakDaoAdressebeskyttelse(private val datasource: DataSource) {

    fun finnSakerMedGraderingOgSkjerming(fnr: String): List<SakMedGraderingOgSkjermet> {
        datasource.connection.use {
            val statement = it.prepareStatement("SELECT id, adressebeskyttelse, erSkjermet from sak where fnr = ?")
            statement.setString(1, fnr)
            return statement.executeQuery().toList {
                SakMedGraderingOgSkjermet(
                    id = getLong(1),
                    adressebeskyttelseGradering = getString(2)?.let { AdressebeskyttelseGradering.valueOf(it) },
                    erSkjermet = getBoolean(3)
                )
            }
        }
    }

    fun hentSakMedGraderingOgSkjerming(id: Long): SakMedGraderingOgSkjermet? {
        datasource.connection.use {
            val statement = it.prepareStatement("SELECT id, adressebeskyttelse, erSkjermet from sak where id = ?")
            statement.setLong(1, id)
            return statement.executeQuery().singleOrNull {
                SakMedGraderingOgSkjermet(
                    id = getLong(1),
                    adressebeskyttelseGradering = getString(2)?.let { AdressebeskyttelseGradering.valueOf(it) },
                    erSkjermet = getBoolean(3)
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

    fun hentSakMedGarderingOgSkjermingPaaBehandling(behandlingId: String): SakMedGraderingOgSkjermet? {
        datasource.connection.use {
            val statement = it.prepareStatement(
                "SELECT s.id, adressebeskyttelse, erSkjermet FROM behandling b" +
                    " INNER JOIN sak s ON b.sak_id = s.id WHERE b.id = ?::uuid"
            )
            statement.setString(1, behandlingId)
            return statement.executeQuery().singleOrNull {
                SakMedGraderingOgSkjermet(
                    id = getLong(1),
                    adressebeskyttelseGradering = getString(2)?.let { AdressebeskyttelseGradering.valueOf(it) },
                    erSkjermet = getBoolean(3)
                )
            }
        }
    }
}