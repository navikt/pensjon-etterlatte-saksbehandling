package no.nav.etterlatte.behandling.aktivitetsplikt

import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.behandling.hendelse.setLong
import no.nav.etterlatte.brev.model.BrevID
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
                        SELECT oppgave_id, sak_id, utbetaling, redusert_etter_inntekt, skal_sende_brev, brev_id from aktivitetsplikt_brevdata
                        WHERE oppgave_id = ?
                        """.trimIndent(),
                    )
                stmt.setObject(1, oppgaveId)
                stmt.executeQuery().singleOrNull {
                    AktivitetspliktInformasjonBrevdata(
                        oppgaveId = getUUID("oppgave_id"),
                        sakid = SakId(getLong("sak_id")),
                        brevId = if (getObject("brev_id") == null) null else getLong("brev_id"),
                        utbetaling = if (getObject("utbetaling") == null) null else getBoolean("utbetaling"),
                        redusertEtterInntekt =
                            if (getObject("redusert_etter_inntekt") ==
                                null
                            ) {
                                null
                            } else {
                                getBoolean("redusert_etter_inntekt")
                            },
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
                        INSERT INTO aktivitetsplikt_brevdata(sak_id, oppgave_id, utbetaling, redusert_etter_inntekt, skal_sende_brev)
                        VALUES(?, ?, ?, ?, ?) 
                        ON CONFLICT (oppgave_id) 
                        DO UPDATE SET utbetaling = excluded.utbetaling, redusert_etter_inntekt = excluded.redusert_etter_inntekt, skal_sende_brev = excluded.skal_sende_brev
                        """.trimIndent(),
                    )
                stmt.setLong(1, data.sakid.sakId)
                stmt.setObject(2, data.oppgaveId)
                stmt.setObject(3, data.utbetaling)
                stmt.setObject(4, data.redusertEtterInntekt)
                stmt.setBoolean(5, data.skalSendeBrev)
                stmt.executeUpdate()
            }
        }

    fun lagreBrevId(
        oppgaveId: UUID,
        brevId: BrevID,
    ) = connectionAutoclosing.hentConnection { connection ->
        with(connection) {
            val stmt =
                prepareStatement(
                    """
                    UPDATE aktivitetsplikt_brevdata SET brev_id = ?
                        WHERE oppgave_id = ? 
                    
                    """.trimIndent(),
                )
            stmt.setLong(1, brevId)
            stmt.setObject(2, oppgaveId)
            stmt.executeUpdate()
        }
    }
}
