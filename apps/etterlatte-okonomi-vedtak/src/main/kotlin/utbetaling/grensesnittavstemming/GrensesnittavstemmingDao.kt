package no.nav.etterlatte.utbetaling.grensesnittavstemming

import kotliquery.Row
import kotliquery.param
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.su.se.bakover.common.toTidspunkt
import java.sql.Timestamp
import javax.sql.DataSource

class GrensesnittavstemmingDao(private val dataSource: DataSource) {

    fun opprettAvstemming(grensesnittavstemming: Grensesnittavstemming): Int =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    INSERT INTO avstemming (id, opprettet, fra_og_med, til, antall_avstemte_oppdrag)
                    VALUES (:id, :opprettet, :fra_og_med, :til, :antall_avstemte_oppdrag)
                    """,
                paramMap = mapOf(
                    "id" to grensesnittavstemming.id.param<String>(),
                    "opprettet" to Timestamp.from(grensesnittavstemming.opprettet.instant).param<Timestamp>(),
                    "fra_og_med" to Timestamp.from(grensesnittavstemming.fraOgMed.instant).param<Timestamp>(),
                    "til" to Timestamp.from(grensesnittavstemming.til.instant).param<Timestamp>(),
                    "antall_avstemte_oppdrag" to grensesnittavstemming.antallAvstemteOppdrag.param<Int>()
                )
            )
                .let { session.run(it.asUpdate) }
                .also { require(it == 1) { "Kunne ikke opprette avstemming" } }
        }

    fun hentSisteAvstemming(): Grensesnittavstemming? =
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
        Grensesnittavstemming(
            id = row.string("id"),
            opprettet = row.instant("opprettet").toTidspunkt(),
            fraOgMed = row.instant("fra_og_med").toTidspunkt(),
            til = row.instant("til").toTidspunkt(),
            antallAvstemteOppdrag = row.int("antall_avstemte_oppdrag")
        )
}