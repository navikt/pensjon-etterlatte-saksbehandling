package no.nav.etterlatte.behandling.aktivitetsplikt

import no.nav.etterlatte.brev.model.Aktivitetsgrad
import no.nav.etterlatte.brev.model.AktivitetspliktInformasjon10mndBrevdata
import no.nav.etterlatte.brev.model.NasjonalEllerUtland
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.database.singleOrNull
import java.util.UUID

class AktivitetspliktBrevDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun hentBrevdata(oppgaveId: UUID): AktivitetspliktInformasjon10mndBrevdata? =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT aktivitetsgrad, utbetaling, redusertEtterInntekt, nasjonalEllerUtland from aktivitetsplikt_brevdata
                        WHERE oppgave_id = ?
                        """.trimIndent(),
                    )
                stmt.setObject(1, oppgaveId)
                stmt.executeQuery().singleOrNull {
                    AktivitetspliktInformasjon10mndBrevdata(
                        aktivitetsgrad = getString("aktivitetsgrad").let { Aktivitetsgrad.valueOf(it) },
                        utbetaling = getBoolean("utbetaling"),
                        redusertEtterInntekt = getBoolean("redusertEtterInntekt"),
                        nasjonalEllerUtland = getString("nasjonalEllerUtland").let { NasjonalEllerUtland.valueOf(it) },
                    )
                }
            }
        }

    fun lagreBrevdata(
        oppgaveId: UUID,
        sakId: SakId,
        data: AktivitetspliktInformasjon10mndBrevdata,
    ) = connectionAutoclosing.hentConnection { connection ->
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
            stmt.setObject(2, sakId.sakId)
            stmt.setObject(3, oppgaveId)
            stmt.setObject(4, data.aktivitetsgrad.name)
            stmt.setBoolean(5, data.utbetaling)
            stmt.setBoolean(6, data.redusertEtterInntekt)
            stmt.setString(7, data.nasjonalEllerUtland.name)
            stmt.executeUpdate()
        }
    }
}
