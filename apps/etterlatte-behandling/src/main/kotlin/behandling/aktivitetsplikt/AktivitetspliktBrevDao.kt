package no.nav.etterlatte.behandling.aktivitetsplikt

import no.nav.etterlatte.behandling.hendelse.getUUID
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
                        SELECT oppgave_id, sak_id, utbetaling, redusert_etter_inntekt, skal_sende_brev from aktivitetsplikt_brevdata
                        WHERE oppgave_id = ?
                        """.trimIndent(),
                    )
                stmt.setObject(1, oppgaveId)
                stmt.executeQuery().singleOrNull {
                    AktivitetspliktInformasjonBrevdata(
                        oppgaveId = getUUID("oppgave_id"),
                        sakid = SakId(getLong("sak_id")),
                        utbetaling = getBoolean("utbetaling"),
                        redusertEtterInntekt = getBoolean("redusert_etter_inntekt"),
                        skalSendeBrev = getBoolean("skal_sende_brev"),
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
                        INSERT INTO aktivitetsplikt_brevdata(id, sak_id, oppgave_id, utbetaling, redusert_etter_inntekt, skal_sende_brev)
                        VALUES(?, ?, ?, ?, ?, ?) 
                        ON CONFLICT (oppgave_id) 
                        DO UPDATE SET utbetaling = excluded.utbetaling, redusert_etter_inntekt = excluded.redusert_etter_inntekt, skal_sende_brev = excluded.skal_sende_brev
                        """.trimIndent(),
                    )
                stmt.setObject(1, UUID.randomUUID())
                stmt.setLong(2, data.sakid.sakId)
                stmt.setObject(3, data.oppgaveId)
                stmt.setObject(4, data.utbetaling)
                stmt.setObject(5, data.redusertEtterInntekt)
                stmt.setBoolean(6, data.skalSendeBrev)
                stmt.executeUpdate()
            }
        }
}
