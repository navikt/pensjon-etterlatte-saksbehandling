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
import javax.sql.DataSource


data class UtbetalingsoppdragNotFoundException(override val message: String) : RuntimeException(message)

class UtbetalingsoppdragDao(private val dataSource: DataSource) {

    fun hentUtbetalingsoppdrag(vedtakId: String): Utbetalingsoppdrag? =
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                "SELECT id, vedtak_id, behandling_id, sak_id, oppdrag, vedtak, status, oppdrag_id, kvittering " +
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
                        oppdrag = getString("oppdrag").let(Jaxb::toOppdrag),
                        vedtak = getString("vedtak").let { vedtak -> objectMapper.readValue(vedtak) },
                        status = getString("status").let(UtbetalingsoppdragStatus::valueOf),
                        oppdragId = getString("oppdrag_id"),
                        kvittering = getString("kvittering")?.let(Jaxb::toOppdrag),
                    )
                }
            }
        }

    private fun hentUtbetalingsoppdragNonNull(vedtakId: String): Utbetalingsoppdrag =
        hentUtbetalingsoppdrag(vedtakId)
            ?: throw UtbetalingsoppdragNotFoundException("Oppdrag for vedtak med vedtakId=$vedtakId finnes ikke")

    fun opprettUtbetalingsoppdrag(vedtak: Vedtak, oppdrag: Oppdrag) =
        dataSource.connection.use { connection ->
            logger.info("Oppretter utbetalingsoppdrag for vedtakId=${vedtak.vedtakId}")

            val stmt = connection.prepareStatement(
                "INSERT INTO utbetalingsoppdrag(vedtak_id, behandling_id, sak_id, oppdrag, vedtak, status) " +
                        "VALUES(?, ?, ?, ?, ?, ?)"
            )

            stmt.use {
                it.setString(1, vedtak.vedtakId)
                it.setString(2, vedtak.behandlingsId)
                it.setString(3, vedtak.sakId)
                it.setString(4, Jaxb.toXml(oppdrag))
                it.setString(5, vedtak.toJson())
                it.setString(6, UtbetalingsoppdragStatus.MOTTATT.name)

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
                "UPDATE utbetalingsoppdrag SET kvittering = ?, oppdrag_id = ? WHERE vedtak_id = ?"
            )

            stmt.use {
                it.setString(1, Jaxb.toXml(oppdragMedKvittering))
                it.setString(2, oppdragMedKvittering.oppdrag110.oppdragsId.toString())
                it.setString(3, oppdragMedKvittering.vedtakId())

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

    companion object {
        private val logger = LoggerFactory.getLogger(UtbetalingsoppdragDao::class.java)
    }
}
