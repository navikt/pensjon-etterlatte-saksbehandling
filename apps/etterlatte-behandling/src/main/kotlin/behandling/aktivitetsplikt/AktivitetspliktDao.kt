package no.nav.etterlatte.behandling.aktivitetsplikt

import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.behandling.AktivitetspliktOppfolging
import no.nav.etterlatte.libs.common.behandling.OpprettAktivitetspliktOppfolging
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.database.toList
import java.util.UUID

class AktivitetspliktDao(private val connectionAutoclosing: ConnectionAutoclosing) {
    fun finnSenesteAktivitetspliktOppfolging(behandlingId: UUID): AktivitetspliktOppfolging? {
        return connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                |SELECT id, 
                | behandling_id,
                | aktivitet,
                | opprettet,
                | opprettet_av
                |FROM aktivitetsplikt_oppfolging
                |WHERE behandling_id = ?
                |ORDER BY id DESC
                |LIMIT 1
                        """.trimMargin(),
                    )
                stmt.setObject(1, behandlingId)
                stmt.executeQuery().toList {
                    AktivitetspliktOppfolging(
                        behandlingId = getUUID("behandling_id"),
                        aktivitet = getString("aktivitet"),
                        opprettet = getTidspunkt("opprettet"),
                        opprettetAv = getString("opprettet_av"),
                    )
                }.firstOrNull()
            }
        }
    }

    fun lagre(
        behandlingId: UUID,
        nyOppfolging: OpprettAktivitetspliktOppfolging,
        navIdent: String,
    ) {
        return connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
            |INSERT INTO aktivitetsplikt_oppfolging(behandling_id, aktivitet, opprettet_av) 
            |VALUES (?, ?, ?)
                        """.trimMargin(),
                    )
                stmt.setObject(1, behandlingId)
                stmt.setString(2, nyOppfolging.aktivitet)
                stmt.setString(3, navIdent)

                stmt.executeUpdate()
            }
        }
    }
}
