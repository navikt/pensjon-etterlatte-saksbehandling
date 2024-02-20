package no.nav.etterlatte.sak

import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import javax.sql.DataSource

/*
    Hvis denne skal gjøre skriveoperasjoner MÅ ingen av metodene her være wrappet i en intransaction, da vil skriveoperasjonen bli overskridet av transaksjonen til intransaction
 */
class SakTilgangDao(private val datasource: DataSource) {
    fun finnSakerMedGraderingOgSkjerming(fnr: String): List<SakMedGraderingOgSkjermet> {
        datasource.connection.use { connection ->
            val statement = connection.prepareStatement("SELECT id, adressebeskyttelse, erSkjermet, enhet from sak where fnr = ?")
            statement.setString(1, fnr)
            return statement.executeQuery().toList {
                SakMedGraderingOgSkjermet(
                    id = getLong(1),
                    adressebeskyttelseGradering = getString(2)?.let { AdressebeskyttelseGradering.valueOf(it) },
                    erSkjermet = getBoolean(3),
                    enhetNr = getString(4),
                )
            }
        }
    }

    fun hentSakMedGraderingOgSkjerming(id: Long): SakMedGraderingOgSkjermet? {
        datasource.connection.use { connection ->
            val statement = connection.prepareStatement("SELECT id, adressebeskyttelse, erSkjermet, enhet from sak where id = ?")
            statement.setLong(1, id)
            return statement.executeQuery().singleOrNull {
                SakMedGraderingOgSkjermet(
                    id = getLong(1),
                    adressebeskyttelseGradering = getString(2)?.let { AdressebeskyttelseGradering.valueOf(it) },
                    erSkjermet = getBoolean(3),
                    enhetNr = getString(4),
                )
            }
        }
    }

    fun hentSakMedGraderingOgSkjermingPaaBehandling(behandlingId: String): SakMedGraderingOgSkjermet? {
        datasource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    "select id, adressebeskyttelse, erSkjermet, enhet from sak where id =" +
                        " (select sak_id from behandling where id = ?::uuid" +
                        " union select sak_id from tilbakekreving where id = ?::uuid" +
                        " union select sak_id from klage where id = ?::uuid)",
                )
            statement.setString(1, behandlingId)
            statement.setString(2, behandlingId)
            statement.setString(3, behandlingId)
            return statement.executeQuery().singleOrNull {
                SakMedGraderingOgSkjermet(
                    id = getLong(1),
                    adressebeskyttelseGradering = getString(2)?.let { AdressebeskyttelseGradering.valueOf(it) },
                    erSkjermet = getBoolean(3),
                    enhetNr = getString(4),
                )
            }
        }
    }

    fun hentSakMedGraderingOgSkjermingPaaOppgave(oppgaveId: String): SakMedGraderingOgSkjermet? {
        datasource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    SELECT s.id as sak_id, adressebeskyttelse, erskjermet, s.enhet 
                    FROM oppgave o
                    INNER JOIN sak s on o.sak_id = s.id
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
                    enhetNr = getString(4),
                )
            }
        }
    }

    fun hentSakMedGraderingOgSkjermingPaaKlage(klageId: String): SakMedGraderingOgSkjermet? {
        datasource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    SELECT s.id as sak_id, adressebeskyttelse, erskjermet, enhet 
                    FROM klage k
                    INNER JOIN sak s on k.sak_id = s.id
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
                    enhetNr = getString(4),
                )
            }
        }
    }
}
