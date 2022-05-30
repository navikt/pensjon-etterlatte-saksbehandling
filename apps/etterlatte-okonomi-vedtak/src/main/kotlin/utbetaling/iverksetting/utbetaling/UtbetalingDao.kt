package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Parameter
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.utbetaling.common.Tidspunkt
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragJaxb
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.vedtakId
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import java.sql.Timestamp
import java.util.*
import javax.sql.DataSource


data class UtbetalingNotFoundException(override val message: String) : RuntimeException(message)

class UtbetalingDao(private val dataSource: DataSource) {

    fun opprettUtbetaling(utbetaling: Utbetaling) =
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                logger.info("Oppretter utbetaling for vedtakId=${utbetaling.vedtakId}")

                queryOf(
                    statement = """
                        INSERT INTO utbetaling(id, vedtak_id, behandling_id, sak_id, oppdrag, status, vedtak, 
                            opprettet, avstemmingsnoekkel, endret, stoenadsmottaker, saksbehandler, attestant)
                        VALUES(:id, :vedtakId, :behandlingId, :sakId, :oppdrag, :status, :vedtak, :opprettet, 
                            :avstemmingsnoekkel, :endret, :stoenadsmottaker, :saksbehandler, :attestant)
                        """,
                    paramMap = mapOf(
                        "id" to utbetaling.id.toString(),
                        "vedtakId" to utbetaling.vedtakId.value,
                        "behandlingId" to utbetaling.behandlingId.value,
                        "sakId" to utbetaling.sakId.value,
                        "status" to UtbetalingStatus.SENDT.name,
                        "vedtak" to utbetaling.vedtak.toJson(),
                        "opprettet" to Timestamp.from(utbetaling.opprettet.instant),
                        "avstemmingsnoekkel" to Timestamp.from(utbetaling.avstemmingsnoekkel.instant),
                        "endret" to Timestamp.from(utbetaling.endret.instant),
                        "stoenadsmottaker" to utbetaling.stoenadsmottaker.value,
                        "saksbehandler" to utbetaling.saksbehandler.value,
                        "attestant" to utbetaling.attestant.value,
                        "oppdrag" to utbetaling.oppdrag?.let { o -> OppdragJaxb.toXml(o) },
                    )
                ).let { tx.run(it.asUpdate) }

                utbetaling.utbetalingslinjer.forEach { utbetalingslinje ->
                    opprettUtbetalingslinje(utbetalingslinje, tx)
                }
            }
        }.let { hentUtbetalingNonNull(utbetaling.vedtakId.value) }

    private fun opprettUtbetalingslinje(
        utbetalingslinje: Utbetalingslinje,
        tx: TransactionalSession
    ) {
        queryOf(
            statement = """
                INSERT INTO utbetalingslinje(id, type, utbetaling_id, erstatter_id, opprettet, periode_fra, periode_til, 
                    beloep, sak_id)
                VALUES(:id, :type, :utbetaling_id, :erstatter_id, :opprettet, :periode_fra, :periode_til,  
                    :beloep, :sak_id)
            """,
            paramMap = mapOf(
                "id" to Parameter<Long>(utbetalingslinje.id.value, Long::class.java),
                "type" to utbetalingslinje.type.name,
                "utbetaling_id" to utbetalingslinje.utbetalingId.toString(),
                "erstatter_id" to utbetalingslinje.erstatterId,
                "opprettet" to Timestamp.from(utbetalingslinje.opprettet.instant),
                "sak_id" to utbetalingslinje.sakId.value,
                "periode_fra" to utbetalingslinje.periode.fra,
                "periode_til" to utbetalingslinje.periode.til,
                "beloep" to utbetalingslinje.beloep,
            )
        ).let { tx.run(it.asUpdate) }
    }

    fun hentUtbetaling(vedtakId: Long): Utbetaling? =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    SELECT id, vedtak_id, behandling_id, sak_id, status, vedtak, opprettet, avstemmingsnoekkel, endret, 
                        stoenadsmottaker, oppdrag, kvittering, kvittering_beskrivelse, kvittering_alvorlighetsgrad, 
                        kvittering_kode, saksbehandler, attestant 
                    FROM utbetaling 
                    WHERE vedtak_id = :vedtakId
                    """,
                paramMap = mapOf("vedtakId" to vedtakId)
            )
                .let {
                    session.run(it.map { row ->
                        val utbetalingslinjer = hentUtbetalingslinjerForUtbetaling(row.string("id"))
                        toUtbetaling(row, utbetalingslinjer)
                    }.asSingle)
                }
        }

    private fun hentUtbetalingslinjerForUtbetaling(utbetalingId: String): List<Utbetalingslinje> =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    SELECT id, type, utbetaling_id, erstatter_id, opprettet, periode_fra, periode_til, beloep, sak_id
                    FROM utbetalingslinje 
                    WHERE utbetaling_id = :utbetalingId
                    """,
                paramMap = mapOf("utbetalingId" to utbetalingId)
            )
                .let { session.run(it.map(::toUtbetalingslinje).asList) }
        }

    fun hentUtbetalinger(fraOgMed: Tidspunkt, til: Tidspunkt): List<Utbetaling> =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    SELECT id, vedtak_id, behandling_id, sak_id, status, vedtak, opprettet, avstemmingsnoekkel, endret, 
                        stoenadsmottaker, oppdrag, kvittering, kvittering_beskrivelse, kvittering_alvorlighetsgrad,  
                        kvittering_kode, saksbehandler, attestant
                    FROM utbetaling
                    WHERE avstemmingsnoekkel >= :fraOgMed AND avstemmingsnoekkel < :til
                    """,
                paramMap = mapOf(
                    "fraOgMed" to Timestamp.from(fraOgMed.instant),
                    "til" to Timestamp.from(til.instant)
                )
            )
                .let {
                    session.run(it.map { row ->
                        val utbetalingslinjer = hentUtbetalingslinjerForUtbetaling(row.string("id"))
                        toUtbetaling(row, utbetalingslinjer)
                    }.asList)
                }
        }

    fun hentUtbetalinger(sakId: Long): List<Utbetaling> =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    SELECT id, vedtak_id, behandling_id, sak_id, status, vedtak, opprettet, avstemmingsnoekkel, endret, 
                        stoenadsmottaker, oppdrag, kvittering, kvittering_beskrivelse, kvittering_alvorlighetsgrad, 
                        kvittering_kode, saksbehandler, attestant
                    FROM utbetaling
                    WHERE sak_id = :sakId
                    """,
                paramMap = mapOf(
                    "sakId" to sakId,
                )
            )
                .let {
                    session.run(it.map { row ->
                        val utbetalingslinjer = hentUtbetalingslinjerForUtbetaling(row.string("id"))
                        toUtbetaling(row, utbetalingslinjer)
                    }.asList)
                }
        }

    fun oppdaterStatus(vedtakId: Long, status: UtbetalingStatus, endret: Tidspunkt) =
        using(sessionOf(dataSource)) { session ->
            logger.info("Oppdaterer status i utbetaling for vedtakId=$vedtakId til $status")

            queryOf(
                statement = """
                    UPDATE utbetaling SET status = :status, endret = :endret WHERE vedtak_id = :vedtakId
                    """,
                paramMap = mapOf(
                    "status" to status.name,
                    "endret" to Timestamp.from(endret.instant),
                    "vedtakId" to vedtakId
                )
            )
                .let { session.run(it.asUpdate) }
                .also { require(it == 1) { "Kunne ikke oppdatere status i utbetaling" } }
                .let { hentUtbetalingNonNull(vedtakId) }
        }

    fun oppdaterKvittering(oppdragMedKvittering: Oppdrag, endret: Tidspunkt) =
        using(sessionOf(dataSource)) { session ->
            logger.info("Oppdaterer kvittering i utbetaling for vedtakId=${oppdragMedKvittering.vedtakId()}")

            queryOf(
                statement = """
                    UPDATE utbetaling 
                    SET kvittering = :kvittering, kvittering_beskrivelse = :beskrivelse, 
                        kvittering_alvorlighetsgrad = :alvorlighetsgrad, kvittering_kode = :kode, 
                        endret = :endret 
                    WHERE vedtak_id = :vedtakId
                    """,
                paramMap = mapOf(
                    "kvittering" to OppdragJaxb.toXml(oppdragMedKvittering),
                    "beskrivelse" to oppdragMedKvittering.mmel.beskrMelding,
                    "alvorlighetsgrad" to oppdragMedKvittering.mmel.alvorlighetsgrad,
                    "kode" to oppdragMedKvittering.mmel.kodeMelding,
                    "endret" to Timestamp.from(endret.instant),
                    "vedtakId" to oppdragMedKvittering.vedtakId()
                )
            )
                .let { session.run(it.asUpdate) }
                .also { require(it == 1) { "Kunne ikke oppdatere kvittering i utbetaling" } }
                .let { hentUtbetalingNonNull(oppdragMedKvittering.vedtakId()) }
        }

    private fun hentUtbetalingNonNull(vedtakId: Long): Utbetaling =
        hentUtbetaling(vedtakId)
            ?: throw UtbetalingNotFoundException("Utbetaling for vedtak med vedtakId=$vedtakId finnes ikke")

    private fun toUtbetaling(row: Row, utbetalingslinjer: List<Utbetalingslinje>) =
        with(row) {
            Utbetaling(
                id = string("id").let { UUID.fromString(it) },
                sakId = SakId(long("sak_id")),
                behandlingId = BehandlingId(string("behandling_id")),
                vedtakId = VedtakId(long("vedtak_id")),
                status = string("status").let(UtbetalingStatus::valueOf),
                opprettet = Tidspunkt(sqlTimestamp("opprettet").toInstant()),
                endret = Tidspunkt(sqlTimestamp("endret").toInstant()),
                avstemmingsnoekkel = Tidspunkt(sqlTimestamp("avstemmingsnoekkel").toInstant()),
                stoenadsmottaker = Foedselsnummer(string("stoenadsmottaker")),
                saksbehandler = NavIdent(string("saksbehandler")),
                attestant = NavIdent(string("attestant")),
                vedtak = string("vedtak").let { vedtak -> objectMapper.readValue(vedtak) },
                oppdrag = string("oppdrag").let(OppdragJaxb::toOppdrag),
                kvittering = stringOrNull("kvittering")?.let {
                    Kvittering(
                        oppdrag = OppdragJaxb.toOppdrag(it),
                        alvorlighetsgrad = string("kvittering_alvorlighetsgrad"),
                        beskrivelse = stringOrNull("kvittering_beskrivelse"),
                        kode = stringOrNull("kvittering_kode")
                    )
                },
                utbetalingslinjer = utbetalingslinjer
            )
        }

    private fun toUtbetalingslinje(row: Row) =
        with(row) {
            Utbetalingslinje(
                id = UtbetalingslinjeId(long("id")),
                type = string("type").let { Utbetalingslinjetype.valueOf(it) },
                utbetalingId = string("utbetaling_id").let { UUID.fromString(it) },
                erstatterId = longOrNull("erstatter_id")?.let { UtbetalingslinjeId(it) },
                opprettet = Tidspunkt(sqlTimestamp("opprettet").toInstant()),
                sakId = SakId(long("sak_id")),
                periode = PeriodeForUtbetaling(
                    fra = localDate("periode_fra"),
                    til = localDateOrNull("periode_til"),
                ),
                beloep = bigDecimalOrNull("beloep"),
            )
        }

    companion object {
        private val logger = LoggerFactory.getLogger(UtbetalingDao::class.java)
    }
}
