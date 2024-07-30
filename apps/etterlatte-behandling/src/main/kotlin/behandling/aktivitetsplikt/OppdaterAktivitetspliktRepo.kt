package no.nav.etterlatte.behandling.aktivitetsplikt

import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.database.toList

class OppdaterAktivitetspliktRepo(
    private val datasource: ConnectionAutoclosing,
) {
    fun hentSakerSomIkkeErSendt(limit: Int): List<Long> =
        datasource.hentConnection { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    SELECT sak_id from resend_aktivitetsplikt_statistikk
                    WHERE status = ? LIMIT ?
                    """.trimIndent(),
                )

            statement.setString(1, OppdaterAktivitetspliktStatus.IKKE_SENDT.name)
            statement.setInt(2, limit)
            val saker =
                statement.executeQuery().toList {
                    getLong("sak_id")
                }
            return@hentConnection saker
        }

    fun oppdaterSakSendt(
        sakId: Long,
        status: OppdaterAktivitetspliktStatus,
    ) {
        datasource.hentConnection { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    UPDATE resend_aktivitetsplikt_statistikk SET status = ? WHERE sak_id = ?
                    """.trimIndent(),
                )
            statement.setString(1, status.name)
            statement.setLong(2, sakId)
            statement.executeUpdate()
        }
    }
}

enum class OppdaterAktivitetspliktStatus {
    IKKE_SENDT,
    SENDT,
    FEIL_I_SENDING,
}
