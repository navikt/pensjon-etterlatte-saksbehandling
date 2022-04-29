package no.nav.etterlatte.oppdrag

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.common.Jaxb
import no.nav.etterlatte.domain.Utbetalingsoppdrag
import no.nav.etterlatte.domain.UtbetalingsoppdragStatus
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import javax.sql.DataSource


data class UtbetalingsoppdragNotFoundException(override val message: String) : RuntimeException(message)

class UtbetalingsoppdragDao(private val dataSource: DataSource) {

    fun hentUtbetalingsoppdrag(vedtakId: String): Utbetalingsoppdrag? =
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                "SELECT id, vedtak_id, behandling_id, sak_id, status, vedtak, opprettet, avstemmingsnoekkel, endret, foedselsnummer, utgaaende_oppdrag, oppdrag_kvittering, beskrivelse_oppdrag, feilkode_oppdrag, meldingkode_oppdrag " +
                        "FROM utbetalingsoppdrag " +
                        "WHERE vedtak_id = ?"
            )

            stmt.use {
                it.setObject(1, vedtakId)

                it.executeQuery().singleOrNull {
                    Utbetalingsoppdrag(
                        id = getInt("id"),
                        vedtakId = getString("vedtak_id"),
                        behandlingId = getString("behandling_id"),
                        sakId = getString("sak_id"),
                        status = getString("status").let(UtbetalingsoppdragStatus::valueOf),
                        vedtak = getString("vedtak").let { vedtak -> objectMapper.readValue(vedtak) },
                        opprettet = getTimestamp("opprettet").toLocalDateTime(),
                        avstemmingsnoekkel = getTimestamp("avstemmingsnoekkel").toLocalDateTime(),
                        endret = getTimestamp("endret").toLocalDateTime(),
                        foedselsnummer = getString("foedselsnummer"),
                        utgaaendeOppdrag = getString("utgaaende_oppdrag").let(Jaxb::toOppdrag),
                        oppdragKvittering = getString("oppdrag_kvittering")?.let(Jaxb::toOppdrag),
                        beskrivelseOppdrag = getString("beskrivelse_oppdrag"),
                        feilkodeOppdrag = getString("feilkode_oppdrag"),
                        meldingKodeOppdrag = getString("meldingkode_oppdrag")
                    )
                }
            }
        }

    fun hentAlleUtbetalingsoppdragMellom(fraOgMed: LocalDateTime, tilOgMed: LocalDateTime): List<Utbetalingsoppdrag> =
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                "SELECT id, vedtak_id, behandling_id, sak_id, status, vedtak, opprettet, avstemmingsnoekkel, endret, foedselsnummer, utgaaende_oppdrag, oppdrag_kvittering, beskrivelse_oppdrag, feilkode_oppdrag, meldingkode_oppdrag " +
                        "FROM utbetalingsoppdrag " +
                        "WHERE avstemmingsnoekkel BETWEEN ? AND ?"
            )

            stmt.use {
                it.setTimestamp(1, Timestamp.valueOf(fraOgMed))
                it.setTimestamp(2, Timestamp.valueOf(tilOgMed))

                it.executeQuery().toList {
                    Utbetalingsoppdrag(
                        id = getInt("id"),
                        vedtakId = getString("vedtak_id"),
                        behandlingId = getString("behandling_id"),
                        sakId = getString("sak_id"),
                        status = getString("status").let(UtbetalingsoppdragStatus::valueOf),
                        vedtak = getString("vedtak").let { vedtak -> objectMapper.readValue(vedtak) },
                        opprettet = getTimestamp("opprettet").toLocalDateTime(),
                        avstemmingsnoekkel = getTimestamp("avstemmingsnoekkel").toLocalDateTime(),
                        endret = getTimestamp("endret").toLocalDateTime(),
                        foedselsnummer = getString("foedselsnummer"),
                        utgaaendeOppdrag = getString("utgaaende_oppdrag").let(Jaxb::toOppdrag),
                        oppdragKvittering = getString("oppdrag_kvittering")?.let(Jaxb::toOppdrag),
                        beskrivelseOppdrag = getString("beskrivelse_oppdrag"),
                        feilkodeOppdrag = getString("feilkode_oppdrag"),
                        meldingKodeOppdrag = getString("meldingkode_oppdrag")
                    )
                }
            }
        }


    private fun hentUtbetalingsoppdragNonNull(vedtakId: String): Utbetalingsoppdrag =
        hentUtbetalingsoppdrag(vedtakId)
            ?: throw UtbetalingsoppdragNotFoundException("Oppdrag for vedtak med vedtakId=$vedtakId finnes ikke")

    fun opprettUtbetalingsoppdrag(vedtak: Vedtak, oppdrag: Oppdrag, opprettetTidspunkt: LocalDateTime) =
        dataSource.connection.use { connection ->
            logger.info("Oppretter utbetalingsoppdrag for vedtakId=${vedtak.vedtakId}")

            val stmt = connection.prepareStatement(
                "INSERT INTO utbetalingsoppdrag(vedtak_id, behandling_id, sak_id, utgaaende_oppdrag, status, vedtak, opprettet, avstemmingsnoekkel, endret, foedselsnummer) " +
                        "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )

            stmt.use {
                it.setString(1, vedtak.vedtakId)
                it.setString(2, vedtak.behandlingsId)
                it.setString(3, vedtak.sakId)
                it.setString(4, Jaxb.toXml(oppdrag))
                it.setString(5, UtbetalingsoppdragStatus.SENDT.name)
                it.setString(6, vedtak.toJson())
                it.setTimestamp(7, Timestamp.valueOf(opprettetTidspunkt))
                it.setTimestamp(8, Timestamp.valueOf(opprettetTidspunkt))
                it.setTimestamp(9, Timestamp.valueOf(opprettetTidspunkt))
                it.setString(10, vedtak.sakIdGjelderFnr)



                require(it.executeUpdate() == 1)
            }
        }.let { hentUtbetalingsoppdragNonNull(vedtak.vedtakId) }

    fun oppdaterStatus(vedtakId: String, status: UtbetalingsoppdragStatus) =
        dataSource.connection.use { connection ->
            logger.info("Oppdaterer status i utbetalingsoppdrag for vedtakId=$vedtakId til $status")

            val stmt = connection.prepareStatement(
                "UPDATE utbetalingsoppdrag SET status = ? WHERE vedtak_id = ?"
            )

            stmt.use {
                it.setString(1, status.name)
                it.setString(2, vedtakId)

                require(it.executeUpdate() == 1)
            }
        }.let { hentUtbetalingsoppdragNonNull(vedtakId) }

    fun oppdaterKvittering(oppdragMedKvittering: Oppdrag) =
        dataSource.connection.use { connection ->
            requireNotNull(oppdragMedKvittering.mmel) { "Oppdrag innholdt ikke kvitteringsmelding" }

            logger.info("Oppdaterer kvittering i utbetalingsoppdrag for vedtakId=${oppdragMedKvittering.vedtakId()}")

            val stmt = connection.prepareStatement(
                "UPDATE utbetalingsoppdrag SET oppdrag_kvittering = ?, beskrivelse_oppdrag = ?, feilkode_oppdrag = ?, meldingkode_oppdrag = ? WHERE vedtak_id = ?"
            )

            stmt.use {
                it.setString(1, Jaxb.toXml(oppdragMedKvittering))
                it.setString(2, oppdragMedKvittering.mmel.beskrMelding)
                it.setString(3, oppdragMedKvittering.mmel.kodeMelding)
                it.setString(4, oppdragMedKvittering.mmel.alvorlighetsgrad)
                it.setString(5, oppdragMedKvittering.vedtakId())

                require(it.executeUpdate() == 1)
            }
        }.let { hentUtbetalingsoppdragNonNull(oppdragMedKvittering.vedtakId()) }

    private fun <T> ResultSet.singleOrNull(block: ResultSet.() -> T): T? {
        return if (next()) {
            block().also {
                require(!next()) { "Skal være unik" }
            }
        } else {
            null
        }
    }

    fun <T> ResultSet.toList(block: ResultSet.() -> T): List<T> {
        return generateSequence {
            if (next()) block()
            else null
        }.toList()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UtbetalingsoppdragDao::class.java)
    }
}
