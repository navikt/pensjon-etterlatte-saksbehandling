package no.nav.etterlatte.sak

import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.SakMedGraderingOgSkjermet
import no.nav.etterlatte.libs.database.setSakId
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.ResultSet
import javax.sql.DataSource

class SakTilgangDao(
    private val datasource: DataSource,
) {
    fun finnSakerMedGraderingOgSkjerming(fnr: String): List<SakMedGraderingOgSkjermet> {
        datasource.connection.use { connection ->
            val statement =
                connection.prepareStatement("SELECT id, adressebeskyttelse, erSkjermet, enhet from sak where fnr = ?")
            statement.setString(1, fnr)
            return statement.executeQuery().toList {
                toSakMedGraderingOgSkjermet()
            }
        }
    }

    fun hentSakMedGraderingOgSkjerming(id: SakId): SakMedGraderingOgSkjermet? {
        datasource.connection.use { connection ->
            val statement =
                connection.prepareStatement("SELECT id, adressebeskyttelse, erSkjermet, enhet from sak where id = ?")
            statement.setSakId(1, id)
            return statement.executeQuery().singleOrNull { toSakMedGraderingOgSkjermet() }
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
            return statement.executeQuery().singleOrNull { toSakMedGraderingOgSkjermet() }
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
            return statement.executeQuery().singleOrNull { toSakMedGraderingOgSkjermet() }
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
            return statement.executeQuery().singleOrNull { toSakMedGraderingOgSkjermet() }
        }
    }

    fun hentSakMedGraderingOgSkjermingPaaGenerellbehandling(generellbehandlingId: String): SakMedGraderingOgSkjermet? {
        datasource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    SELECT s.id as sak_id, adressebeskyttelse, erskjermet, enhet 
                    FROM generellbehandling g
                    INNER JOIN sak s on g.sak_id = s.id
                    WHERE g.id = ?::uuid
                    """.trimIndent(),
                )
            statement.setString(1, generellbehandlingId)
            return statement.executeQuery().singleOrNull { toSakMedGraderingOgSkjermet() }
        }
    }

    fun hentSakMedGraderingOgSkjermingPaaTilbakekreving(tilbakekrevingId: String): SakMedGraderingOgSkjermet? {
        datasource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    SELECT s.id as sak_id, adressebeskyttelse, erskjermet, enhet 
                    FROM tilbakekreving t
                    INNER JOIN sak s on t.sak_id = s.id
                    WHERE t.id = ?::uuid
                    """.trimIndent(),
                )
            statement.setString(1, tilbakekrevingId)
            return statement.executeQuery().singleOrNull { toSakMedGraderingOgSkjermet() }
        }
    }

    fun hentSakMedGraderingOgSkjermingPaaEtteroppgjoer(etteroppgjoerId: String): SakMedGraderingOgSkjermet? {
        datasource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    SELECT s.id as sak_id, adressebeskyttelse, erskjermet, enhet 
                    FROM etteroppgjoer_behandling e
                    INNER JOIN sak s on e.sak_id = s.id
                    WHERE e.id = ?::uuid
                    """.trimIndent(),
                )
            statement.setString(1, etteroppgjoerId)
            return statement.executeQuery().singleOrNull { toSakMedGraderingOgSkjermet() }
        }
    }

    private fun ResultSet.toSakMedGraderingOgSkjermet() =
        SakMedGraderingOgSkjermet(
            id = SakId(getLong("sak_id")),
            adressebeskyttelseGradering =
                getString("adressebeskyttelse")?.let {
                    AdressebeskyttelseGradering.valueOf(it)
                },
            erSkjermet = getBoolean("erskjermet"),
            enhetNr = Enhetsnummer.nullable(getString("enhet")),
        )
}
