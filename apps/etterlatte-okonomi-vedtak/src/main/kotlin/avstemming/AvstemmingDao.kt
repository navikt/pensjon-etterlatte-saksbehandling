package no.nav.etterlatte.avstemming

import kotliquery.Row
import kotliquery.param
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.sql.Timestamp
import javax.sql.DataSource

class AvstemmingDao(private val dataSource: DataSource) {

    fun opprettAvstemming(avstemming: Avstemming): Int =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    INSERT INTO avstemming (id, opprettet, fra_og_med, til, antall_avstemte_oppdrag)
                    VALUES (:id, :opprettet, :fra_og_med, :til, :antall_avstemte_oppdrag)
                    """,
                paramMap = mapOf(
                    "id" to avstemming.id.param<String>(),
                    "opprettet" to Timestamp.valueOf(avstemming.opprettet).param<Timestamp>(),
                    "fra_og_med" to Timestamp.valueOf(avstemming.fraOgMed).param<Timestamp>(),
                    "til" to Timestamp.valueOf(avstemming.til).param<Timestamp>(),
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
                    SELECT id, opprettet, fra_og_med, til, antall_avstemte_oppdrag 
                    FROM avstemming 
                    ORDER BY til DESC
                    LIMIT 1
                    """
            )
                .let { session.run(it.map(::toAvstemming).asSingle) }
        }

    private fun toAvstemming(row: Row) =
        Avstemming(
            id = row.string("id"),
            opprettet = row.localDateTime("opprettet"),
            fraOgMed = row.localDateTime("fra_og_med"),
            til = row.localDateTime("til"),
            antallAvstemteOppdrag = row.int("antall_avstemte_oppdrag")
        )
}