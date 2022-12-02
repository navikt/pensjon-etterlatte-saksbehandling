package no.nav.etterlatte.utbetaling.grensesnittavstemming

import kotliquery.Row
import kotliquery.param
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import java.sql.Timestamp
import javax.sql.DataSource

class AvstemmingDao(private val dataSource: DataSource) {

    fun opprettGrensesnittavstemming(grensesnittavstemming: Grensesnittavstemming): Int =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    INSERT INTO avstemming (id, opprettet, periode_fra, periode_til, antall_oppdrag, avstemmingsdata,
                        avstemmingtype)
                    VALUES (:id, :opprettet, :periode_fra, :periode_til, :antall_oppdrag, :avstemmingsdata,
                        :avstemmingtype)
                    """,
                paramMap = mapOf(
                    "id" to grensesnittavstemming.id.value.param(),
                    "opprettet" to Timestamp.from(grensesnittavstemming.opprettet.instant).param(),
                    "periode_fra" to Timestamp.from(grensesnittavstemming.periode.fraOgMed.instant).param(),
                    "periode_til" to Timestamp.from(grensesnittavstemming.periode.til.instant).param(),
                    "antall_oppdrag" to grensesnittavstemming.antallOppdrag.param(),
                    "avstemmingsdata" to grensesnittavstemming.avstemmingsdata.param(),
                    "avstemmingtype" to Avstemmingtype.GRENSESNITTAVSTEMMING.name.param()
                )
            )
                .let { session.run(it.asUpdate) }
                .also { require(it == 1) { "Kunne ikke opprette avstemming" } }
        }

    fun hentSisteGrensesnittavstemming(): Grensesnittavstemming? =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    SELECT id, opprettet, periode_fra, periode_til, antall_oppdrag, avstemmingsdata
                    FROM avstemming 
                    WHERE avstemmingtype = :avstemmingtype
                    ORDER BY periode_til DESC 
                    LIMIT 1
                    """,
                paramMap = mapOf("avstemmingtype" to Avstemmingtype.GRENSESNITTAVSTEMMING.name)
            )
                .let { session.run(it.map(::toGrensesnittavstemming).asSingle) }
        }

    private fun toGrensesnittavstemming(row: Row) =
        Grensesnittavstemming(
            id = UUIDBase64(row.string("id")),
            opprettet = row.instant("opprettet").toTidspunkt(),
            periode = Avstemmingsperiode(
                fraOgMed = row.instant("periode_fra").toTidspunkt(),
                til = row.instant("periode_til").toTidspunkt()
            ),
            antallOppdrag = row.int("antall_oppdrag"),
            avstemmingsdata = row.string("avstemmingsdata")
        )
}