package no.nav.etterlatte.oppgave

import no.nav.etterlatte.DatabaseExtension
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
class OppgaveIndekserTest(
    private val dataSource: DataSource,
) {
    @Test
    fun `migrerer gyldige indekser som brukes av selektive oppgavesoek`() {
        dataSource.connection.use { connection ->
            val sakId =
                connection.createStatement().use { statement ->
                    statement
                        .executeQuery(
                            "INSERT INTO sak (fnr, saktype, enhet) VALUES ('indeks-test', 'BARNEPENSJON', '4806') RETURNING id",
                        ).use {
                            it.next()
                            it.getLong(1)
                        }
                }
            connection
                .prepareStatement(
                    """
                    INSERT INTO oppgave (id, sak_id, referanse, gruppe_id, type, fnr, kilde, status, frist)
                    SELECT gen_random_uuid(), ?, 'referanse-' || n, 'gruppe-' || n, 'FOERSTEGANGSBEHANDLING',
                        'person-' || n, 'BEHANDLING', CASE WHEN n % 100 = 0 THEN 'PAA_VENT' ELSE 'FERDIGSTILT' END,
                        TIMESTAMP '2024-01-01' + n * INTERVAL '1 day'
                    FROM generate_series(1, 10000) AS n
                    """.trimIndent(),
                ).use {
                    it.setLong(1, sakId)
                    it.executeUpdate()
                }
            connection.createStatement().use { it.execute("ANALYZE oppgave") }

            val soek =
                mapOf(
                    "oppgave_referanse_idx" to "referanse = 'referanse-500'",
                    "oppgave_gruppe_id_type_idx" to
                        "gruppe_id = 'gruppe-500' AND type = 'FOERSTEGANGSBEHANDLING' AND NOT (status = ANY('{FERDIGSTILT,FEILREGISTRERT,AVBRUTT}'::text[]))",
                    "oppgave_fnr_idx" to "fnr = 'person-500'",
                    "oppgave_ventefrist_idx" to
                        "frist <= TIMESTAMP '2025-01-01' AND type = ANY('{FOERSTEGANGSBEHANDLING}'::text[]) AND kilde = ANY('{BEHANDLING}'::text[]) AND status = 'PAA_VENT'",
                )
            for ((indeks, betingelse) in soek) {
                connection.prepareStatement("SELECT indisvalid AND indisready FROM pg_index WHERE indexrelid = ?::regclass").use {
                    it.setString(1, indeks)
                    it.executeQuery().use { resultat ->
                        assertTrue(resultat.next())
                        assertTrue(resultat.getBoolean(1), "$indeks skal vaere gyldig og klar")
                    }
                }
                val plan =
                    connection.createStatement().use { statement ->
                        statement.executeQuery("EXPLAIN (ANALYZE, BUFFERS) SELECT id FROM oppgave WHERE $betingelse").use { resultat ->
                            buildString {
                                while (resultat.next()) {
                                    appendLine(resultat.getString(1))
                                }
                            }
                        }
                    }
                assertTrue(plan.contains(indeks), "Forventet $indeks i planen:\n$plan")
            }
        }
    }
}
