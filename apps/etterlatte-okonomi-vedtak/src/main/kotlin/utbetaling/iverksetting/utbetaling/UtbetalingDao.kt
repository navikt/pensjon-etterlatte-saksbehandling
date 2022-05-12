package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.utbetaling.common.Tidspunkt
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragJaxb
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.vedtakId
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.sql.Timestamp
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
                FROM utbetalingsoppdrag 
                WHERE vedtak_id = ?
            """
            )

            stmt.use {
                it.setObject(1, vedtakId)

                it.executeQuery().singleOrNull(toUtbetaling())
            }
        }

    fun hentAlleUtbetalingerMellom(fraOgMed: Tidspunkt, til: Tidspunkt): List<Utbetaling> =
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                """
                SELECT id, vedtak_id, behandling_id, sak_id, status, vedtak, opprettet, avstemmingsnoekkel, endret, 
                    foedselsnummer, utgaaende_oppdrag, oppdrag_kvittering, beskrivelse_oppdrag, feilkode_oppdrag, 
                    meldingkode_oppdrag
                FROM utbetalingsoppdrag
                WHERE avstemmingsnoekkel >= ? AND avstemmingsnoekkel < ?
                """
            )

            stmt.use {
                it.setTimestamp(1, Timestamp.from(fraOgMed.instant), tzUTC)
                it.setTimestamp(2, Timestamp.from(til.instant), tzUTC)

                it.executeQuery().toList(toUtbetaling())
            }
        }


    private fun hentUtbetalingNonNull(vedtakId: String): Utbetaling =
        hentUtbetaling(vedtakId)
            ?: throw UtbetalingNotFoundException("Utbetaling for vedtak med vedtakId=$vedtakId finnes ikke")

    fun opprettUtbetaling(vedtak: Vedtak, oppdrag: Oppdrag, opprettetTidspunkt: Tidspunkt) =
        dataSource.connection.use { connection ->
            logger.info("Oppretter utbetaling for vedtakId=${vedtak.vedtakId}")

            val stmt = connection.prepareStatement(
                """
                INSERT INTO utbetalingsoppdrag(vedtak_id, behandling_id, sak_id, utgaaende_oppdrag, status, vedtak, 
                    opprettet, avstemmingsnoekkel, endret, foedselsnummer)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """
            )

            stmt.use {
                it.setString(1, vedtak.vedtakId)
                it.setString(2, vedtak.behandlingsId)
                it.setString(3, vedtak.sakId)
                it.setString(4, OppdragJaxb.toXml(oppdrag))
                it.setString(5, UtbetalingStatus.SENDT.name)
                it.setString(6, vedtak.toJson())
                it.setTimestamp(7, Timestamp.from(opprettetTidspunkt.instant), tzUTC)
                it.setTimestamp(8, Timestamp.from(opprettetTidspunkt.instant), tzUTC)
                it.setTimestamp(9, Timestamp.from(opprettetTidspunkt.instant), tzUTC)
                it.setString(10, vedtak.sakIdGjelderFnr)



                require(it.executeUpdate() == 1)
            }
        }.let { hentUtbetalingNonNull(vedtak.vedtakId) }

    fun oppdaterStatus(vedtakId: String, status: UtbetalingStatus, endret: Tidspunkt) =
        dataSource.connection.use { connection ->
            logger.info("Oppdaterer status i utbetaling for vedtakId=$vedtakId til $status")

            val stmt = connection.prepareStatement(
                "UPDATE utbetalingsoppdrag SET status = ?, endret = ? WHERE vedtak_id = ?"
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
                UPDATE utbetalingsoppdrag 
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
                it.setString(6, oppdragMedKvittering.vedtakId())

                require(it.executeUpdate() == 1)
            }
        }.let { hentUtbetalingNonNull(oppdragMedKvittering.vedtakId()) }


    private fun toUtbetaling(): ResultSet.() -> Utbetaling =
        {
            Utbetaling(
                id = getLong("id"),
                sakId = SakId(getString("sak_id")),
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
                kvitteringMeldingKode = getString("meldingkode_oppdrag")
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
