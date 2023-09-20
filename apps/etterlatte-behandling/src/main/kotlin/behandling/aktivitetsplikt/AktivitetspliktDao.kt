package no.nav.etterlatte.behandling.aktivitetsplikt

import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.libs.common.behandling.AktivitetspliktOppfolging
import no.nav.etterlatte.libs.common.behandling.OpprettAktivitetspliktOppfolging
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.database.toList
import java.sql.Connection
import java.util.UUID

class AktivitetspliktDao(private val connection: () -> Connection) {
    fun finnSenesteAktivitetspliktOppfolging(behandlingId: UUID): AktivitetspliktOppfolging? {
        val stmt =
            connection().prepareStatement(
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
        return stmt.executeQuery().toList {
            AktivitetspliktOppfolging(
                behandlingId = getUUID("behandling_id"),
                aktivitet = getString("aktivitet"),
                opprettet = getTidspunkt("opprettet"),
                opprettetAv = getString("opprettet_av"),
            )
        }.firstOrNull()
    }

    fun lagre(
        behandlingId: UUID,
        nyOppfolging: OpprettAktivitetspliktOppfolging,
        navIdent: String,
    ) {
        val stmt =
            connection().prepareStatement(
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
