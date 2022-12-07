package no.nav.etterlatte.utbetaling.grensesnittavstemming

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.param
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.utbetaling.avstemming.Konsistensavstemming
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import java.sql.Timestamp
import javax.sql.DataSource

class AvstemmingDao(private val dataSource: DataSource) {

    fun opprettKonsistensavstemming(konsistensavstemming: Konsistensavstemming): Int =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    INSERT INTO avstemming (id, opprettet, loepende_fom, opprettet_tom, avstemmingsdata, 
                        avstemmingtype, saktype, loepende_utbetalinger)
                    VALUES (:id, :opprettet, :loepende_fom, :opprettet_tom, :avstemmingsdata,
                        :avstemmingtype, :saktype, :loepende_utbetalinger)
                    """,
                paramMap = mapOf(
                    "id" to konsistensavstemming.id.value.param(),
                    "opprettet" to Timestamp.from(konsistensavstemming.opprettet.instant).param(),
                    "loepende_fom" to Timestamp.from(konsistensavstemming.loependeFraOgMed.instant).param(),
                    "opprettet_tom" to Timestamp.from(konsistensavstemming.opprettetTilOgMed.instant).param(),
                    "avstemmingsdata" to konsistensavstemming.avstemmingsdata.param(),
                    "avstemmingtype" to Avstemmingtype.KONSISTENSAVSTEMMING.name.param(),
                    "saktype" to konsistensavstemming.sakType.name.param(),
                    "loepende_utbetalinger" to konsistensavstemming.loependeUtbetalinger.toJson()
                )
            ).let {
                session.run(
                    it.asUpdate
                )
            }.also { require(it == 1) { "Kunne ikke opprette avstemming" } }
        }

    fun hentSisteKonsistensavsvemming(saktype: Saktype): Konsistensavstemming? =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    SELECT id, opprettet, loepende_fom, opprettet_tom, antall_oppdrag, avstemmingsdata, 
                         avstemmingtype, saktype, loepende_utbetalinger 
                    FROM avstemming 
                    WHERE avstemmingtype = :avstemmingtype
                    AND saktype = :saktype
                    ORDER BY opprettet_tom DESC 
                    LIMIT 1
                    """,
                paramMap = mapOf(
                    "avstemmingtype" to Avstemmingtype.KONSISTENSAVSTEMMING.name.param(),
                    "saktype" to saktype.name.param()
                )
            ).let { session.run(it.map(::toKonsistensavstemming).asSingle) }
        }

    private fun toKonsistensavstemming(row: Row) = Konsistensavstemming(
        id = UUIDBase64(row.string("id")),
        sakType = row.string("saktype").let { Saktype.fraString(it) },
        opprettet = row.instant("opprettet").toTidspunkt(),
        avstemmingsdata = row.string("avstemmingsdata"),
        loependeFraOgMed = row.instant("loepende_fom").toTidspunkt(),
        opprettetTilOgMed = row.instant("opprettet_tom").toTidspunkt(),
        loependeUtbetalinger = objectMapper.readValue(row.string("loepende_utbetalinger"))
    )

    fun opprettGrensesnittavstemming(grensesnittavstemming: Grensesnittavstemming): Int =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    INSERT INTO avstemming (id, opprettet, periode_fra, periode_til, antall_oppdrag, avstemmingsdata,
                        avstemmingtype, saktype)
                    VALUES (:id, :opprettet, :periode_fra, :periode_til, :antall_oppdrag, :avstemmingsdata,
                        :avstemmingtype, :saktype)
                    """,
                paramMap = mapOf(
                    "id" to grensesnittavstemming.id.value.param(),
                    "opprettet" to Timestamp.from(grensesnittavstemming.opprettet.instant).param(),
                    "periode_fra" to Timestamp.from(grensesnittavstemming.periode.fraOgMed.instant).param(),
                    "periode_til" to Timestamp.from(grensesnittavstemming.periode.til.instant).param(),
                    "antall_oppdrag" to grensesnittavstemming.antallOppdrag.param(),
                    "avstemmingsdata" to grensesnittavstemming.avstemmingsdata.param(),
                    "avstemmingtype" to Avstemmingtype.GRENSESNITTAVSTEMMING.name.param(),
                    "saktype" to grensesnittavstemming.saktype.name.param()
                )
            ).let { session.run(it.asUpdate) }.also { require(it == 1) { "Kunne ikke opprette avstemming" } }
        }

    fun hentSisteGrensesnittavstemming(saktype: Saktype): Grensesnittavstemming? =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    SELECT id, opprettet, periode_fra, periode_til, antall_oppdrag, avstemmingsdata, 
                         avstemmingtype, saktype
                    FROM avstemming 
                    WHERE avstemmingtype = :avstemmingtype
                    AND saktype = :saktype
                    ORDER BY periode_til DESC 
                    LIMIT 1
                    """,
                paramMap = mapOf(
                    "avstemmingtype" to Avstemmingtype.GRENSESNITTAVSTEMMING.name.param(),
                    "saktype" to saktype.name.param()
                )
            ).let { session.run(it.map(::toGrensesnittavstemming).asSingle) }
        }

    private fun toGrensesnittavstemming(row: Row) = Grensesnittavstemming(
        id = UUIDBase64(row.string("id")),
        opprettet = row.instant("opprettet").toTidspunkt(),
        periode = Avstemmingsperiode(
            fraOgMed = row.instant("periode_fra").toTidspunkt(),
            til = row.instant("periode_til").toTidspunkt()
        ),
        antallOppdrag = row.int("antall_oppdrag"),
        avstemmingsdata = row.string("avstemmingsdata"),
        saktype = row.string("saktype").let { Saktype.fraString(it) }
    )
}