package no.nav.etterlatte.attestering

import no.nav.etterlatte.domain.AttestasjonsStatus
import no.nav.etterlatte.domain.AttestertVedtak
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime

data class AttestertVedtakNotFoundException(override val message: String) : RuntimeException(message)

class AttestasjonDao(private val connection: () -> Connection) {

    fun hentAttestertVedtak(vedtakId: String): AttestertVedtak? =
        connection().use { connection ->
            val stmt = connection.prepareStatement(
                "SELECT vedtak_id, attestant_id, tidspunkt, status " +
                        "FROM attestasjon " +
                        "WHERE vedtak_id = ?"
            )

            stmt.use {
                it.setString(1, vedtakId)

                it.executeQuery().singleOrNull {
                    AttestertVedtak(
                        vedtakId = getString("vedtak_id"),
                        attestantId = getString("attestant_id") ?: null,
                        tidspunkt = getTimestamp("tidspunkt")?.toLocalDateTime(),
                        status = getString("status").let { status ->
                            AttestasjonsStatus.valueOf(
                                status
                            )
                        }
                    )
                }
            }
        }

    fun attesterVedtak(vedtakId: String, attestasjon: Attestasjon): AttestertVedtak =
        connection().use { connection ->
            val stmt = connection.prepareStatement(
                "UPDATE attestasjon " +
                        "SET status = ?::status, attestant_id = ?, tidspunkt = ?" +
                        "WHERE vedtak_id = ? "
            )

            stmt.use {
                it.setString(1, AttestasjonsStatus.ATTESTERT.name)
                it.setString(2, attestasjon.attestantId)
                it.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()))
                it.setString(4, vedtakId)

                require(it.executeUpdate() == 1)

            }
        }.let { hentAttestertVedtakNonNull(vedtakId) }

    fun settAttestertVedtakTilIkkeAttestert(vedtakId: String): AttestertVedtak =
        connection().use { connection ->
            val stmt = connection.prepareStatement(
                "UPDATE attestasjon " +
                        "SET status = ?::status " +
                        "WHERE vedtak_id = ? "
            )

            stmt.use {
                it.setString(1, AttestasjonsStatus.IKKE_ATTESTERT.name)
                it.setString(2, vedtakId)

                require(it.executeUpdate() == 1)
            }
        }.let { hentAttestertVedtakNonNull(vedtakId) }


    private fun hentAttestertVedtakNonNull(vedtakId: String): AttestertVedtak =
        hentAttestertVedtak(vedtakId)
            ?: throw AttestertVedtakNotFoundException("Attestert Vedtak for vedtak med vedtakId=$vedtakId finnes ikke")


    fun opprettMottattVedtak(vedtak: Vedtak) =
        connection().use { connection ->
            val stmt = connection.prepareStatement(
                "INSERT INTO attestasjon(vedtak_id, status)" +
                        "VALUES (?, ?::status)"
            )

            stmt.use {
                it.setString(1, vedtak.vedtakId)
                it.setString(2, AttestasjonsStatus.TIL_ATTESTERING.name)

                require(it.executeUpdate() == 1)
            }
        }.let { hentAttestertVedtakNonNull(vedtak.vedtakId) }

    fun opprettAttestertVedtak(vedtak: Vedtak, attestasjon: Attestasjon) =
        connection().use { connection ->
            val stmt = connection.prepareStatement(
                "INSERT INTO attestasjon(vedtak_id, attestant_id, tidspunkt, status)" +
                        "VALUES (?, ?, ?, ?::status)"
            )

            stmt.use {
                it.setString(1, vedtak.vedtakId)
                it.setString(2, attestasjon.attestantId)
                it.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()))
                it.setString(4, AttestasjonsStatus.ATTESTERT.name)

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
