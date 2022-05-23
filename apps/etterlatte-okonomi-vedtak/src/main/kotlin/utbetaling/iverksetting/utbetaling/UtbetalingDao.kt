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

    fun hentUtbetaling(vedtakId: Long): Utbetaling? =
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                """
                SELECT id, vedtak_id, behandling_id, sak_id, status, vedtak, opprettet, avstemmingsnoekkel, endret, 
                    foedselsnummer, utgaaende_oppdrag, oppdrag_kvittering, beskrivelse_oppdrag, feilkode_oppdrag, 
                    meldingkode_oppdrag, saksbehandler, attestant 
                FROM utbetaling 
                WHERE vedtak_id = ?
            """
            )

            stmt.use {
                it.setLong(1, vedtakId)

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
                SELECT id, type, utbetaling_id, erstatter_id, opprettet, periode_fra, periode_til, beloep, sak_id
                FROM utbetalingslinje 
                WHERE utbetaling_id = ?
            """
            )

            stmt.use {
                it.setString(1, utbetalingId)

                it.executeQuery().toList(this::toUtbetalingslinje)
            }
        }

    fun hentAlleUtbetalingerMellom(fraOgMed: Tidspunkt, til: Tidspunkt): List<Utbetaling> =
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                """
                SELECT id, vedtak_id, behandling_id, sak_id, status, vedtak, opprettet, avstemmingsnoekkel, endret, 
                    foedselsnummer, utgaaende_oppdrag, oppdrag_kvittering, beskrivelse_oppdrag, feilkode_oppdrag, 
                    meldingkode_oppdrag, saksbehandler, attestant
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

    private fun hentUtbetalingNonNull(vedtakId: Long): Utbetaling =
        hentUtbetaling(vedtakId)
            ?: throw UtbetalingNotFoundException("Utbetaling for vedtak med vedtakId=$vedtakId finnes ikke")

    fun opprettUtbetaling(utbetaling: Utbetaling) =
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                logger.info("Oppretter utbetaling for vedtakId=${utbetaling.vedtakId}")

                queryOf(
                    statement = """
                        INSERT INTO utbetaling(id, vedtak_id, behandling_id, sak_id, utgaaende_oppdrag, status, vedtak, 
                            opprettet, avstemmingsnoekkel, endret, foedselsnummer, saksbehandler, attestant)
                        VALUES(:id, :vedtakId, :behandlingId, :sakId, :oppdrag, :status, :vedtak, :opprettet, 
                            :avstemmingsnoekkel, :endret, :foedselsnummer, :saksbehandler, :attestant)
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
                        "foedselsnummer" to utbetaling.stoenadsmottaker.value,
                        "saksbehandler" to utbetaling.saksbehandler.value,
                        "attestant" to utbetaling.attestant.value
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
                "id" to utbetalingslinje.id.value,
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

    fun oppdaterStatus(vedtakId: Long, status: UtbetalingStatus, endret: Tidspunkt) =
        dataSource.connection.use { connection ->
            logger.info("Oppdaterer status i utbetaling for vedtakId=$vedtakId til $status")

            val stmt = connection.prepareStatement(
                "UPDATE utbetaling SET status = ?, endret = ? WHERE vedtak_id = ?"
            )

            stmt.use {
                it.setString(1, status.name)
                it.setTimestamp(2, Timestamp.from(endret.instant), tzUTC)
                it.setLong(3, vedtakId)

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
                it.setLong(6, oppdragMedKvittering.vedtakId())

                require(it.executeUpdate() == 1)
            }
        }.let { hentUtbetalingNonNull(oppdragMedKvittering.vedtakId()) }


    private fun toUtbetaling(resultSet: ResultSet, utbetalingslinjer: List<Utbetalingslinje>) =
        with(resultSet) {
            Utbetaling(
                id = getString("id").let { UUID.fromString(it) },
                sakId = SakId(getLong("sak_id")),
                behandlingId = BehandlingId(getString("behandling_id")),
                vedtakId = VedtakId(getLong("vedtak_id")),
                status = getString("status").let(UtbetalingStatus::valueOf),
                opprettet = Tidspunkt(getTimestamp("opprettet", tzUTC).toInstant()),
                endret = Tidspunkt(getTimestamp("endret", tzUTC).toInstant()),
                avstemmingsnoekkel = Tidspunkt(getTimestamp("avstemmingsnoekkel", tzUTC).toInstant()),
                stoenadsmottaker = Foedselsnummer(getString("foedselsnummer")),
                saksbehandler = NavIdent(getString("saksbehandler")),
                attestant = NavIdent(getString("attestant")),
                vedtak = getString("vedtak").let { vedtak -> objectMapper.readValue(vedtak) },
                oppdrag = getString("utgaaende_oppdrag").let(OppdragJaxb::toOppdrag),
                kvittering = getString("oppdrag_kvittering")?.let {
                    Kvittering(
                        oppdrag = OppdragJaxb.toOppdrag(it),
                        beskrivelse = getString("beskrivelse_oppdrag"),
                        feilkode = getString("feilkode_oppdrag"),
                        meldingKode = getString("meldingkode_oppdrag")
                    )
                },
                utbetalingslinjer = utbetalingslinjer
            )
        }

    private fun toUtbetalingslinje(resultSet: ResultSet) =
        with(resultSet) {
            Utbetalingslinje(
                id = UtbetalingslinjeId(getLong("id")),
                type = getString("type").let { Utbetalingslinjetype.valueOf(it) },
                utbetalingId = getString("utbetaling_id").let { UUID.fromString(it) },
                erstatterId = UtbetalingslinjeId(getLong("erstatter_id")),
                opprettet = Tidspunkt(getTimestamp("opprettet", tzUTC).toInstant()),
                sakId = SakId(getLong("sak_id")),
                periode = Utbetalingsperiode(
                    fra = getObject("periode_fra", LocalDate::class.java),
                    til = getObject("periode_til", LocalDate::class.java),
                ),
                beloep = getBigDecimal("beloep"),
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
