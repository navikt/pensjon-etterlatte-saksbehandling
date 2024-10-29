package no.nav.etterlatte.behandling.aktivitetsplikt

import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.behandling.hendelse.setLong
import no.nav.etterlatte.brev.model.Aktivitetsgrad
import no.nav.etterlatte.brev.model.NasjonalEllerUtland
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.database.singleOrNull
import java.util.UUID

class AktivitetspliktBrevDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun hentBrevdata(oppgaveId: UUID): AktivitetspliktInformasjonBrevdata? =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT oppgave_id, sak_id, aktivitetsgrad, utbetaling, redusertEtterInntekt, nasjonalEllerUtland from aktivitetsplikt_brevdata
                        WHERE oppgave_id = ?
                        """.trimIndent(),
                    )
                stmt.setObject(1, oppgaveId)
                stmt.executeQuery().singleOrNull {
                    AktivitetspliktInformasjonBrevdata(
                        oppgaveId = getUUID("oppgave_id"),
                        sakid = SakId(getLong("sak_id")),
                        aktivitetsgrad = getString("aktivitetsgrad")?.let { Aktivitetsgrad.valueOf(it) },
                        utbetaling = getBoolean("utbetaling"),
                        redusertEtterInntekt = getBoolean("redusertEtterInntekt"),
                        nasjonalEllerUtland = getString("nasjonalEllerUtland")?.let { NasjonalEllerUtland.valueOf(it) },
                    )
                }
            }
        }

    fun lagreBrevdata(data: AktivitetspliktInformasjonBrevdata) =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val stmt =
                    prepareStatement(
                        """
                        INSERT INTO aktivitetsplikt_brevdata(id, sak_id, oppgave_id, aktivitetsgrad, utbetaling, redusertEtterInntekt, nasjonalEllerUtland)
                        VALUES(?, ?, ?, ?, ?, ?, ?) 
                        ON CONFLICT (oppgave_id) 
                        DO UPDATE SET aktivitetsgrad = excluded.aktivitetsgrad, utbetaling = excluded.utbetaling, redusertEtterInntekt = excluded.redusertEtterInntekt, nasjonalEllerUtland = excluded.nasjonalEllerUtland
                        """.trimIndent(),
                    )
                stmt.setObject(1, UUID.randomUUID())
                stmt.setLong(2, data.sakid.sakId)
                stmt.setObject(3, data.oppgaveId)
                stmt.setObject(4, data.aktivitetsgrad?.name)
                stmt.setObject(5, data.utbetaling)
                stmt.setObject(6, data.redusertEtterInntekt)
                stmt.setString(7, data.nasjonalEllerUtland?.name)
                stmt.executeUpdate()
            }
        }
}
