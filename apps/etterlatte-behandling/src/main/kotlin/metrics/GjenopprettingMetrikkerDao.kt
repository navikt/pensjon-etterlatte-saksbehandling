package no.nav.etterlatte.metrics

import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.database.toList
import java.sql.ResultSet
import javax.sql.DataSource

class GjenopprettingMetrikkerDao(private val dataSource: DataSource) {
    fun gjenopprettinger(): List<Behandlinger> {
        dataSource.connection.use {
            val statement =
                it.prepareStatement(
                    """
                    select count(distinct s.id) antall, type, status, s.enhet,
                       case saksbehandler
                            when 'EY' then 'automatisk'
                            else 'manuell'
                        end automatisk
                    from sak s join oppgave o on s.id = o.sak_id
                    where o.kilde = 'GJENOPPRETTING'
                    group by type, status, s.enhet, automatisk;
                    """.trimIndent(),
                )
            return statement.executeQuery().toList {
                asBehandling()
            }
        }
    }

    fun avbruttGrunnetSoeknad(): List<Int> {
        dataSource.connection.use {
            val statement =
                it.prepareStatement(
                    """
                    select count(distinct b.sak_id) antall from behandling b
                    join oppgave o on b.id::text = o.referanse
                    where b.status = 'AVBRUTT' and b.kilde = 'GJENOPPRETTA'
                    and exists (
                        select 1 from behandling b2
                        where b2.sak_id = b.sak_id and b2.kilde = 'GJENNY' and b2.status != 'AVBRUTT'
                    );
                    """.trimIndent(),
                )
            return statement.executeQuery().toList {
                getInt("antall")
            }
        }
    }

    private fun ResultSet.asBehandling(): Behandlinger {
        return Behandlinger(
            antall = getInt("antall"),
            automatisk = getString("automatisk"),
            status = Status.valueOf(getString("status")),
            enhet = getString("enhet"),
            type = getString("type"),
        )
    }
}

data class Behandlinger(
    val antall: Int,
    val automatisk: String,
    val status: Status,
    val type: String,
    val enhet: String,
)
