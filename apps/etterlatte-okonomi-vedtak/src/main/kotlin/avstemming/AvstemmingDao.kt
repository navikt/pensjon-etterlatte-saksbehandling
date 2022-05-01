package no.nav.etterlatte.avstemming

import kotliquery.Row
import kotliquery.param
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.sql.Timestamp
import java.util.*
import javax.sql.DataSource

class AvstemmingDao(private val dataSource: DataSource) {

    fun opprettAvstemming(avstemming: Avstemming): Int =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    INSERT INTO avstemming (id, opprettet, avstemmingsnoekkel_tom, antall_avstemte_oppdrag)
                    VALUES (:id, :opprettet, :avstemmingsnoekkel_tom, :antall_avstemte_oppdrag)
                    """,
                paramMap = mapOf(
                    "id" to avstemming.id.param<UUID>(),
                    "opprettet" to Timestamp.valueOf(avstemming.opprettet).param<Timestamp>(),
                    "avstemmingsnoekkel_tom" to Timestamp.valueOf(avstemming.avstemmingsnokkelTilOgMed).param<Timestamp>(),
                    "antall_avstemte_oppdrag" to avstemming.antallAvstemteOppdrag.param<Int>()
                )
            )
                .let { session.run(it.asUpdate) }
                .also { require(it == 1) { "Kunne ikke opprette avstemming" } }
        }

    fun hentSisteAvstemming(): Avstemming? =
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
        Avstemming(
            id = row.uuid("id"),
            opprettet = row.localDateTime("opprettet"),
            avstemmingsnokkelTilOgMed = row.localDateTime("avstemmingsnoekkel_tom"),
            antallAvstemteOppdrag = row.int("antall_avstemte_oppdrag")
        )
}