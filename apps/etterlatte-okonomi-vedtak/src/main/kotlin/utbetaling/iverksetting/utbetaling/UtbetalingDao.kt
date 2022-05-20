package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import com.fasterxml.jackson.module.kotlin.readValue
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
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource


data class UtbetalingNotFoundException(override val message: String) : RuntimeException(message)

class UtbetalingDao(private val dataSource: DataSource) {

    fun hentUtbetaling(vedtakId: String): Utbetaling? =
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                """
                SELECT id, vedtak_id, behandling_id, sak_id, status, vedtak, opprettet, avstemmingsnoekkel, endret, 
                    foedselsnummer, utgaaende_oppdrag, oppdrag_kvittering, beskrivelse_oppdrag, feilkode_oppdrag, 
                    meldingkode_oppdrag
                FROM utbetaling 
                WHERE vedtak_id = ?
            """
            )

            stmt.use {
                it.setObject(1, vedtakId)

                it.executeQuery().singleOrNull {
                    val utbetalingslinjer = hentUtbetalingslinjerForUtbetaling(this.getString("id"))
                    toUtbetaling(this, utbetalingslinjer)
                }
            }
        }

    private fun hentUtbetalingslinjerForUtbetaling(utbetalingId: String): List<Utbetalingslinje> =
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                """
                SELECT id, opprettet, periode_fra, periode_til, beloep, utbetaling_id, sak_id, erstatter_id
                FROM utbetalingslinje 
                WHERE utbetaling_id = ?
            """
            )

            stmt.use {
                it.setObject(1, utbetalingId)

                it.executeQuery().toList(this::toUtbetalingslinje)
            }
        }

    fun hentAlleUtbetalingerMellom(fraOgMed: Tidspunkt, til: Tidspunkt): List<Utbetaling> =
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                """
                SELECT id, vedtak_id, behandling_id, sak_id, status, vedtak, opprettet, avstemmingsnoekkel, endret, 
                    foedselsnummer, utgaaende_oppdrag, oppdrag_kvittering, beskrivelse_oppdrag, feilkode_oppdrag, 
                    meldingkode_oppdrag
                FROM utbetaling
                WHERE avstemmingsnoekkel >= ? AND avstemmingsnoekkel < ?
                """
            )

            stmt.use {
                it.setTimestamp(1, Timestamp.from(fraOgMed.instant), tzUTC)
                it.setTimestamp(2, Timestamp.from(til.instant), tzUTC)

                it.executeQuery().toList {
                    val utbetalingslinjer = hentUtbetalingslinjerForUtbetaling(this.getString("id"))
                    toUtbetaling(this, utbetalingslinjer)
                }
            }
        }

    fun hentUtbetalinger(sakId: Long): List<Utbetaling> =
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                """
                SELECT id, vedtak_id, behandling_id, sak_id, status, vedtak, opprettet, avstemmingsnoekkel, endret, 
                    foedselsnummer, utgaaende_oppdrag, oppdrag_kvittering, beskrivelse_oppdrag, feilkode_oppdrag, 
                    meldingkode_oppdrag
                FROM utbetaling
                WHERE sak_id = ?
                """
            )

            stmt.use {
                it.setLong(1, sakId)

                it.executeQuery().toList {
                    val utbetalingslinjer = hentUtbetalingslinjerForUtbetaling(this.getString("id"))
                    toUtbetaling(this, utbetalingslinjer)
                }
            }
        }

    private fun hentUtbetalingNonNull(vedtakId: String): Utbetaling =
        hentUtbetaling(vedtakId)
            ?: throw UtbetalingNotFoundException("Utbetaling for vedtak med vedtakId=$vedtakId finnes ikke")

    fun opprettUtbetaling(utbetaling: Utbetaling) =
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                logger.info("Oppretter utbetaling for vedtakId=${utbetaling.vedtakId}")

                queryOf(
                    statement = """
                        INSERT INTO utbetaling(id, vedtak_id, behandling_id, sak_id, utgaaende_oppdrag, status, vedtak, 
                            opprettet, avstemmingsnoekkel, endret, foedselsnummer)
                        VALUES(:id, :vedtakId, :behandlingId, :sakId, :oppdrag, :status, :vedtak, :opprettet, 
                            :avstemmingsnoekkel, :endret, :foedselsnummer)
                        """,
                    paramMap = mapOf(
                        "id" to utbetaling.id.toString(),
                        "vedtakId" to utbetaling.vedtakId.value,
                        "behandlingId" to utbetaling.behandlingId.value,
                        "sakId" to utbetaling.sakId.value,
                        "oppdrag" to utbetaling.oppdrag?.let { o -> OppdragJaxb.toXml(o) },
                        "status" to UtbetalingStatus.SENDT.name,
                        "vedtak" to utbetaling.vedtak.toJson(),
                        "opprettet" to Timestamp.from(utbetaling.opprettet.instant),
                        "avstemmingsnoekkel" to Timestamp.from(utbetaling.avstemmingsnoekkel.instant),
                        "endret" to Timestamp.from(utbetaling.endret.instant),
                        "foedselsnummer" to utbetaling.foedselsnummer.value
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
                INSERT INTO utbetalingslinje(id, opprettet, periode_fra, periode_til, beloep, utbetaling_id, 
                    sak_id, erstatter_id)
                VALUES(:id, :opprettet, :periode_fra, :periode_til, :beloep, :utbetaling_id, :sak_id, 
                    :erstatter_id)
            """,
            paramMap = mapOf(
                "id" to utbetalingslinje.id,
                "opprettet" to Timestamp.from(utbetalingslinje.opprettet.instant),
                "periode_fra" to utbetalingslinje.periode.fra,
                "periode_til" to utbetalingslinje.periode.til,
                "beloep" to utbetalingslinje.beloep,
                "utbetaling_id" to utbetalingslinje.utbetalingId.toString(),
                "sak_id" to utbetalingslinje.sakId.value,
                "erstatter_id" to utbetalingslinje.erstatterId
            )
        ).let { tx.run(it.asUpdate) }
    }

    fun opprettUtbetaling2(utbetaling: Utbetaling) =
        dataSource.connection.use { connection ->
            logger.info("Oppretter utbetaling for vedtakId=${utbetaling.vedtakId}")

            val stmt = connection.prepareStatement(
                """
                INSERT INTO utbetaling(id, vedtak_id, behandling_id, sak_id, utgaaende_oppdrag, status, vedtak, 
                    opprettet, avstemmingsnoekkel, endret, foedselsnummer)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """
            )

            stmt.use {
                it.setString(1, utbetaling.id.toString())
                it.setString(2, utbetaling.vedtakId.value)
                it.setString(3, utbetaling.behandlingId.value)
                it.setLong(4, utbetaling.sakId.value)
                it.setString(5, utbetaling.oppdrag?.let { o -> OppdragJaxb.toXml(o) })
                it.setString(6, UtbetalingStatus.SENDT.name)
                it.setString(7, utbetaling.vedtak.toJson())
                it.setTimestamp(8, Timestamp.from(utbetaling.opprettet.instant), tzUTC)
                it.setTimestamp(9, Timestamp.from(utbetaling.avstemmingsnoekkel.instant), tzUTC)
                it.setTimestamp(10, Timestamp.from(utbetaling.endret.instant), tzUTC)
                it.setString(11, utbetaling.foedselsnummer.value)

                require(it.executeUpdate() == 1)
            }

            utbetaling.utbetalingslinjer.forEach {
                opprettUtbetalingslinje(it)
            }


        }.let { hentUtbetalingNonNull(utbetaling.vedtakId.value) }

    fun opprettUtbetalingslinje(utbetalingslinje: Utbetalingslinje) =
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                """
                INSERT INTO utbetalingslinje(id, opprettet, periode_fra, periode_til, beloep, utbetaling_id, sak_id, erstatter_id)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?)
            """
            )

            stmt.use {
                it.setString(1, utbetalingslinje.id)
                it.setTimestamp(2, Timestamp.from(utbetalingslinje.opprettet.instant), tzUTC)
                it.setObject(3, utbetalingslinje.periode.fra)
                it.setObject(4, utbetalingslinje.periode.til)
                it.setBigDecimal(5, utbetalingslinje.beloep)
                it.setString(6, utbetalingslinje.utbetalingId.toString())
                it.setLong(7, utbetalingslinje.sakId.value)
                it.setString(8, utbetalingslinje.erstatterId)

                require(it.executeUpdate() == 1)
            }
        }


    fun oppdaterStatus(vedtakId: String, status: UtbetalingStatus, endret: Tidspunkt) =
        dataSource.connection.use { connection ->
            logger.info("Oppdaterer status i utbetaling for vedtakId=$vedtakId til $status")

            val stmt = connection.prepareStatement(
                "UPDATE utbetaling SET status = ?, endret = ? WHERE vedtak_id = ?"
            )

            stmt.use {
                it.setString(1, status.name)
                it.setTimestamp(2, Timestamp.from(endret.instant), tzUTC)
                it.setString(3, vedtakId)

                require(it.executeUpdate() == 1)
            }
        }.let { hentUtbetalingNonNull(vedtakId) }

    fun oppdaterKvittering(oppdragMedKvittering: Oppdrag, endret: Tidspunkt) =
        dataSource.connection.use { connection ->
            requireNotNull(oppdragMedKvittering.mmel) { "Oppdrag innholdt ikke kvitteringsmelding" }

            logger.info("Oppdaterer kvittering i utbetaling for vedtakId=${oppdragMedKvittering.vedtakId()}")

            val stmt = connection.prepareStatement(
                """
                UPDATE utbetaling 
                SET oppdrag_kvittering = ?, beskrivelse_oppdrag = ?, feilkode_oppdrag = ?, 
                    meldingkode_oppdrag = ?, endret = ? 
                WHERE vedtak_id = ?
            """
            )

            stmt.use {
                it.setString(1, OppdragJaxb.toXml(oppdragMedKvittering))
                it.setString(2, oppdragMedKvittering.mmel.beskrMelding)
                it.setString(3, oppdragMedKvittering.mmel.alvorlighetsgrad)
                it.setString(4, oppdragMedKvittering.mmel.kodeMelding)
                it.setTimestamp(5, Timestamp.from(endret.instant), tzUTC)
                it.setString(6, oppdragMedKvittering.vedtakId()

                require(it.executeUpdate() == 1)
            }
        }.let { hentUtbetalingNonNull(oppdragMedKvittering.vedtakId()) }


    private fun toUtbetaling(resultSet: ResultSet, utbetalingslinjer: List<Utbetalingslinje>) =
        with(resultSet) {
            Utbetaling(
                id = getString("id").let { UUID.fromString(it) },
                sakId = SakId(getLong("sak_id")),
                behandlingId = BehandlingId(getString("behandling_id")),
                vedtakId = VedtakId(getString("vedtak_id")),
                status = getString("status").let(UtbetalingStatus::valueOf),
                opprettet = Tidspunkt(getTimestamp("opprettet", tzUTC).toInstant()),
                endret = Tidspunkt(getTimestamp("endret", tzUTC).toInstant()),
                avstemmingsnoekkel = Tidspunkt(getTimestamp("avstemmingsnoekkel", tzUTC).toInstant()),
                foedselsnummer = Foedselsnummer(getString("foedselsnummer")),
                vedtak = getString("vedtak").let { vedtak -> objectMapper.readValue(vedtak) },
                oppdrag = getString("utgaaende_oppdrag").let(OppdragJaxb::toOppdrag),
                kvittering = getString("oppdrag_kvittering")?.let(OppdragJaxb::toOppdrag),
                kvitteringBeskrivelse = getString("beskrivelse_oppdrag"),
                kvitteringFeilkode = getString("feilkode_oppdrag"),
                kvitteringMeldingKode = getString("meldingkode_oppdrag"),
                utbetalingslinjer = utbetalingslinjer
            )
        }

    private fun toUtbetalingslinje(resultSet: ResultSet) =
        with(resultSet) {
            Utbetalingslinje(
                id = getString("id"),
                sakId = SakId(getLong("sak_id")),
                periode = Utbetalingsperiode(
                    fra = getObject("periode_fra", LocalDate::class.java),
                    til = getObject("periode_til", LocalDate::class.java),
                ),
                opprettet = Tidspunkt(getTimestamp("opprettet", tzUTC).toInstant()),
                beloep = getBigDecimal("beloep"),
                erstatterId = getString("erstatter_id"),
                utbetalingId = getString("utbetaling_id").let { UUID.fromString(it) },
            )
        }

    private fun <T> ResultSet.singleOrNull(block: ResultSet.() -> T): T? {
        return if (next()) {
            block().also {
                require(!next()) { "Skal være unik" }
            }
        } else {
            null
        }
    }

    private fun <T> ResultSet.toList(block: ResultSet.() -> T): List<T> {
        return generateSequence {
            if (next()) block()
            else null
        }.toList()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UtbetalingDao::class.java)
        val tzUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    }
}
