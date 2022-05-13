package no.nav.etterlatte.utbetaling.grensesnittavstemming

import kotliquery.Row
import kotliquery.param
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.utbetaling.common.toTidspunkt
import java.sql.Timestamp
import java.util.*
import javax.sql.DataSource

class GrensesnittavstemmingDao(private val dataSource: DataSource) {

    fun opprettAvstemming(grensesnittavstemming: Grensesnittavstemming): Int =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    INSERT INTO avstemming (id, opprettet, periode_fra, periode_til, antall_oppdrag, avstemmingsdata)
                    VALUES (:id, :opprettet, :periode_fra, :periode_til, :antall_oppdrag, :avstemmingsdata)
                    """,
                paramMap = mapOf(
                    "id" to grensesnittavstemming.id.value.param<String>(),
                    "opprettet" to Timestamp.from(grensesnittavstemming.opprettet.instant).param<Timestamp>(),
                    "periode_fra" to Timestamp.from(grensesnittavstemming.periodeFraOgMed.instant).param<Timestamp>(),
                    "periode_til" to Timestamp.from(grensesnittavstemming.periodeTil.instant).param<Timestamp>(),
                    "antall_oppdrag" to grensesnittavstemming.antallOppdrag.param<Int>(),
                    "avstemmingsdata" to grensesnittavstemming.avstemmingsdata.param<String>(),
                )
            )
                .let { session.run(it.asUpdate) }
                .also { require(it == 1) { "Kunne ikke opprette avstemming" } }
        }

    fun hentSisteAvstemming(): Grensesnittavstemming? =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    SELECT id, opprettet, periode_fra, periode_til, antall_oppdrag, avstemmingsdata
                    FROM avstemming 
                    ORDER BY periode_til DESC
                    LIMIT 1
                    """
            )
                .let { session.run(it.map(::toAvstemming).asSingle) }
        }

    private fun toAvstemming(row: Row) =
        Grensesnittavstemming(
            id = UUIDBase64(row.string("id")),
            opprettet = row.instant("opprettet").toTidspunkt(),
            periodeFraOgMed = row.instant("periode_fra").toTidspunkt(),
            periodeTil = row.instant("periode_til").toTidspunkt(),
            antallOppdrag = row.int("antall_oppdrag"),
            avstemmingsdata = row.stringOrNull("avstemmingsdata"),
        )

    companion object {
        val tzUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    }

}