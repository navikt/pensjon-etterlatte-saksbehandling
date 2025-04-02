package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.setSakId
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.ResultSet

class EtteroppgjoerDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun hentEtteroppgjoer(
        sakId: SakId,
        inntektsaar: Int,
    ): Etteroppgjoer? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT e.sak_id, e.inntektsaar, e.status
                        FROM etteroppgjoer e
                        WHERE e.sak_id = ?
                        AND e.inntektsaar = ?
                        """.trimIndent(),
                    )
                statement.setLong(1, sakId.sakId)
                statement.setInt(2, inntektsaar)
                statement.executeQuery().singleOrNull { toEtteroppgjoer() }
            }
        }

    fun hentEtteroppgjoerForStatus(
        status: EtteroppgjoerStatus,
        // TODO sjekke på inntektsaar også?
    ): List<Etteroppgjoer> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT *
                        FROM etteroppgjoer
                        WHERE status = ?
                        AND inntektsaar = ?
                        """.trimIndent(),
                    )
                statement.setString(1, status.name)
                statement.executeQuery().toList { toEtteroppgjoer() }
            }
        }

    private fun ResultSet.toEtteroppgjoer() =
        Etteroppgjoer(
            sakId = SakId(getLong("sak_id")),
            inntektsaar = getInt("inntektsaar"),
            status = EtteroppgjoerStatus.valueOf(getString("status")),
        )

    fun lagerEtteroppgjoer(
        sakId: SakId,
        inntektsaar: Int,
        status: EtteroppgjoerStatus,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val statement =
                prepareStatement(
                    """
                    INSERT INTO etteroppgjoer(
                        sak_id, inntektsaar, opprettet, status
                    ) 
                    VALUES (?, ?, ?, ?) 
                    ON CONFLICT (sak_id, inntektsaar) DO UPDATE SET
                        status = excluded.status
                    """.trimIndent(),
                )
            statement.setSakId(1, sakId)
            statement.setInt(2, inntektsaar)
            statement.setTidspunkt(3, Tidspunkt(java.time.Instant.now()))
            statement.setString(4, status.name)
            statement.executeUpdate().also {
                krev(it == 1) {
                    "Kunne ikke lagre etteroppgjør for sakid $sakId"
                }
            }
        }
    }
}
