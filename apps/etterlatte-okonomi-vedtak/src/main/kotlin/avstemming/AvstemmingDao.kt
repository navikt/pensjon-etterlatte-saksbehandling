package no.nav.etterlatte.avstemming

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.sql.Timestamp
import javax.sql.DataSource

class AvstemmingDao(private val dataSource: DataSource) {

    fun opprettAvstemming(avstemming: NyAvstemming): Int =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    INSERT INTO avstemming (opprettet, avstemmingsnoekkel_tom, antall_avstemte_oppdrag)
                    VALUES (:opprettet, :avstemmingsnoekkel_tom, :antall_avstemte_oppdrag)
                    """,
                paramMap = mapOf(
                    "opprettet" to Timestamp.valueOf(avstemming.opprettet),
                    "avstemmingsnoekkel_tom" to Timestamp.valueOf(avstemming.avstemmingsnokkelTilOgMed),
                    "antall_avstemte_oppdrag" to avstemming.antallAvstemteOppdrag
                )
            )
                .let { session.run(it.asUpdate) }
                .also { require(it == 1) { "Kunne ikke opprette avstemming" } }
        }

    fun hentNyesteAvstemming(): FullfortAvstemming? =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    SELECT id, opprettet, avstemmingsnoekkel_tom, antall_avstemte_oppdrag 
                    FROM avstemming 
                    ORDER BY avstemmingsnoekkel_tom DESC
                    LIMIT 1
                    """
            )
                .let { session.run(it.map(::toAvstemming).asSingle) }
        }

    private fun toAvstemming(row: Row) =
        FullfortAvstemming(
            id = row.long("id"),
            opprettet = row.localDateTime("opprettet"),
            avstemmingsnokkelTilOgMed = row.localDateTime("avstemmingsnoekkel_tom"),
            antallAvstemteOppdrag = row.int("antall_avstemte_oppdrag")
        )
}