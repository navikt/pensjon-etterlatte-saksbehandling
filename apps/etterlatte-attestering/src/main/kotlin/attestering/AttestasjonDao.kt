package no.nav.etterlatte.attestering

import no.nav.etterlatte.domain.Attestasjon
import no.nav.etterlatte.domain.AttestasjonsStatus
import no.nav.etterlatte.domain.AttestertVedtak
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime

data class AttestertVedtakNotFoundException(override val message: String) : RuntimeException(message)

class AttestasjonDao(private val connection: () -> Connection) {

    fun hentAttestertVedtak(vedtakId: String): AttestertVedtak? =
        connection().use { connection ->
            val stmt = connection.prepareStatement(
                "SELECT vedtak_id, attestant_id, attestasjonstidspunkt, attestasjonsstatus " +
                        "FROM attestasjon " +
                        "WHERE vedtak_id = ?"
            )

            stmt.use {
                it.setString(1, vedtakId)

                it.executeQuery().singleOrNull {
                    AttestertVedtak(
                        vedtakId = getString("vedtak_id"),
                        attestantId = getString("attestant_id"),
                        attestasjonstidspunkt = getTimestamp("attestasjonstidspunkt").toLocalDateTime(),
                        attestasjonsstatus = getString("attestasjonsstatus").let { status ->
                            AttestasjonsStatus.valueOf(
                                status
                            )
                        }
                    )
                }
            }
        }

    private fun hentAttestertVedtakNonNull(vedtakId: String): AttestertVedtak =
        hentAttestertVedtak(vedtakId)
            ?: throw AttestertVedtakNotFoundException("Attestert Vedtak for vedtak med vedtakId=$vedtakId finnes ikke")

    fun opprettAttestertVedtak(vedtak: Vedtak, attestasjon: Attestasjon) =
        connection().use { connection ->
            val stmt = connection.prepareStatement(
                "INSERT INTO attestasjon(vedtak_id, attestant_id, attestasjonstidspunkt, attestasjonsstatus)" +
                        "VALUES (?, ?, ?, ?::status)"
            )

            stmt.use {
                it.setString(1, vedtak.vedtakId)
                it.setString(2, attestasjon.attestantId)
                it.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()))
                it.setString(4, AttestasjonsStatus.TIL_ATTESTERING.name)

                require(it.executeUpdate() == 1)
            }
        }.let { hentAttestertVedtakNonNull(vedtak.vedtakId) }


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
