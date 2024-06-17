package no.nav.etterlatte.metrics

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.database.toList
import java.sql.ResultSet
import javax.sql.DataSource

class OppgaveMetrikkerDao(
    private val dataSource: DataSource,
) {
    fun hentOppgaveAntall(): List<OppgaveAntall> =
        dataSource.connection.use {
            val statement =
                it.prepareStatement(
                    """
                    SELECT count(*), status, enhet, saktype
                    FROM oppgave
                    group by status, enhet, saktype
                    """.trimIndent(),
                )
            return statement.executeQuery().toList {
                asOppgaveAntall()
            }
        }

    private fun ResultSet.asOppgaveAntall(): OppgaveAntall =
        OppgaveAntall(
            antall = getInt("count"),
            status = Status.valueOf(getString("status")),
            enhet = getString("enhet"),
            saktype = SakType.valueOf(getString("saktype")),
        )

    fun hentDistinkteSaksbehandlere(): List<SaksbehandlerAntall> =
        dataSource.connection.use {
            val antallTotalt =
                it
                    .prepareStatement(
                        """
                        SELECT count(distinct saksbehandler) antall
                        FROM oppgave
                        """.trimIndent(),
                    ).executeQuery()
                    .toList {
                        SaksbehandlerAntall(
                            antall = getInt("antall"),
                            enhet = "Totalt",
                        )
                    }
            val antallPerEnhet =
                it
                    .prepareStatement(
                        """
                        SELECT count(distinct saksbehandler) antall, enhet
                        FROM oppgave
                        group by enhet;
                        """.trimIndent(),
                    ).executeQuery()
                    .toList {
                        SaksbehandlerAntall(
                            antall = getInt("antall"),
                            enhet = getString("enhet"),
                        )
                    }
            antallTotalt + antallPerEnhet
        }
}

data class OppgaveAntall(
    val antall: Int,
    val status: Status,
    val enhet: String,
    val saktype: SakType,
)

data class SaksbehandlerAntall(
    val antall: Int,
    val enhet: String,
)
