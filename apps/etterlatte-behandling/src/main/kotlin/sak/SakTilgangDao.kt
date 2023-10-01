package no.nav.etterlatte.sak

import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.Connection

class SakTilgangDao(private val connection: () -> Connection) {
    fun finnSakerMedGraderingOgSkjerming(fnr: String): List<SakMedGraderingOgSkjermet> {
        with(connection()) {
            val statement = prepareStatement("SELECT id, adressebeskyttelse, erSkjermet from sak where fnr = ?")
            statement.setString(1, fnr)
            return statement.executeQuery().toList {
                SakMedGraderingOgSkjermet(
                    id = getLong(1),
                    adressebeskyttelseGradering = getString(2)?.let { AdressebeskyttelseGradering.valueOf(it) },
                    erSkjermet = getBoolean(3),
                )
            }
        }
    }

    fun hentSakMedGraderingOgSkjerming(id: Long): SakMedGraderingOgSkjermet? {
        with(connection()) {
            val statement = prepareStatement("SELECT id, adressebeskyttelse, erSkjermet from sak where id = ?")
            statement.setLong(1, id)
            return statement.executeQuery().singleOrNull {
                SakMedGraderingOgSkjermet(
                    id = getLong(1),
                    adressebeskyttelseGradering = getString(2)?.let { AdressebeskyttelseGradering.valueOf(it) },
                    erSkjermet = getBoolean(3),
                )
            }
        }
    }

    fun oppdaterAdresseBeskyttelse(
        id: Long,
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
    ): Int {
        with(connection()) {
            val statement = prepareStatement("UPDATE sak SET adressebeskyttelse = ? where id = ?")
            statement.setString(1, adressebeskyttelseGradering.toString())
            statement.setLong(2, id)
            return statement.executeUpdate()
        }
    }

    fun hentSakMedGarderingOgSkjermingPaaBehandling(behandlingId: String): SakMedGraderingOgSkjermet? {
        with(connection()) {
            val statement =
                prepareStatement(
                    "SELECT s.id, adressebeskyttelse, erSkjermet FROM behandling b" +
                        " INNER JOIN sak s ON b.sak_id = s.id WHERE b.id = ?::uuid",
                )
            statement.setString(1, behandlingId)
            return statement.executeQuery().singleOrNull {
                SakMedGraderingOgSkjermet(
                    id = getLong(1),
                    adressebeskyttelseGradering = getString(2)?.let { AdressebeskyttelseGradering.valueOf(it) },
                    erSkjermet = getBoolean(3),
                )
            }
        }
    }

    fun hentSakMedGraderingOgSkjermingPaaOppgave(oppgaveId: String): SakMedGraderingOgSkjermet? {
        with(connection()) {
            val statement =
                prepareStatement(
                    """
                    SELECT s.id as sak_id, adressebeskyttelse, erskjermet 
                    FROM oppgave o
                    INNER JOIN Sak s on o.sak_id = s.id
                    WHERE o.id = ?::uuid
                    """.trimIndent(),
                )
            statement.setString(1, oppgaveId)
            return statement.executeQuery().singleOrNull {
                SakMedGraderingOgSkjermet(
                    id = getLong("sak_id"),
                    adressebeskyttelseGradering =
                        getString("adressebeskyttelse")?.let {
                            AdressebeskyttelseGradering.valueOf(it)
                        },
                    erSkjermet = getBoolean("erskjermet"),
                )
            }
        }
    }

    fun hentSakMedGraderingOgSkjermingPaaKlage(klageId: String): SakMedGraderingOgSkjermet? {
        with(connection()) {
            val statement =
                prepareStatement(
                    """
                    SELECT s.id as sak_id, adressebeskyttelse, erskjermet 
                    FROM klage k
                    INNER JOIN Sak s on k.sak_id = s.id
                    WHERE k.id = ?::uuid
                    """.trimIndent(),
                )
            statement.setString(1, klageId)
            return statement.executeQuery().singleOrNull {
                SakMedGraderingOgSkjermet(
                    id = getLong("sak_id"),
                    adressebeskyttelseGradering =
                        getString("adressebeskyttelse")?.let {
                            AdressebeskyttelseGradering.valueOf(it)
                        },
                    erSkjermet = getBoolean("erskjermet"),
                )
            }
        }
    }
}
