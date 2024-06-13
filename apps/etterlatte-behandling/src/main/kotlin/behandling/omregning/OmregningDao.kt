package no.nav.etterlatte.behandling.omregning

import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.sak.KjoeringRequest

class OmregningDao(
    private val connection: ConnectionAutoclosing,
) {
    fun oppdaterKjoering(request: KjoeringRequest) {
        connection.hentConnection { connection ->
            with(connection) {
                val statement =
                    prepareStatement(
                        """
                        INSERT INTO omregningskjoering (kjoering, status, sak_id)
                        VALUES (?, ?, ?)
                        """.trimIndent(),
                    )
                statement.setString(1, request.kjoering)
                statement.setString(2, request.status.name)
                statement.setLong(3, request.sakId)
                statement.executeUpdate().also { require(it == 1) }
            }
        }
    }
}
