package no.nav.etterlatte.fordeler

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import javax.sql.DataSource

class FordelerRepository(private val ds: DataSource) {
    private val opprettFordeling =
        """  
        insert into fordelinger (soeknad_id, fordeling) values (?,?)
        """.trimIndent()

    private val opprettKriterier =
        """
        insert into kriterietreff(soeknad_id, kriterie) values (?, ?)
        """.trimIndent()

    private val finnFordeling =
        """
        select soeknad_id, fordeling, opprettet_ts
        from fordelinger
        where soeknad_id = ?
        """.trimIndent()

    private val tellFordelinger =
        """
        select count(1)
        from fordelinger
        where fordeling = ?
        """.trimIndent()

    private val finnKriterier =
        """
        select kriterie
        from kriterietreff
        where soeknad_id = ?
        """.trimIndent()

    fun finnFordeling(id: Long): FordeltRecord? {
        return ds.connection.use {
            it.prepareStatement(finnFordeling).apply {
                setLong(1, id)
            }.executeQuery().singleOrNull {
                FordeltRecord(getLong("soeknad_id"), getString("fordeling"), getTimestamp("opprettet_ts").toTidspunkt())
            }
        }
    }

    fun finnKriterier(id: Long): List<String> {
        return ds.connection.use {
            it.prepareStatement(finnKriterier).apply {
                setLong(1, id)
            }.executeQuery().toList {
                getString(1)
            }
        }
    }

    fun lagreFordeling(unsavedFordeling: FordeltTransient) {
        ds.connection.use { connection ->

            connection.prepareStatement(opprettFordeling).apply {
                setLong(1, unsavedFordeling.soeknadId)
                setString(2, unsavedFordeling.fordeling)
            }.executeUpdate()

            if (unsavedFordeling.kriterier.isNotEmpty()) {
                connection.prepareStatement(opprettKriterier).apply {
                    unsavedFordeling.kriterier.forEach {
                        setLong(1, unsavedFordeling.soeknadId)
                        setString(2, it)
                        addBatch()
                    }
                }.executeBatch()
            }
        }
    }

    fun antallFordeltTil(system: String): Long {
        return ds.connection.use {
            it.prepareStatement(tellFordelinger).apply {
                setString(1, system)
            }.executeQuery().singleOrNull { getLong(1) } ?: 0L
        }
    }
}

data class FordeltTransient(
    val soeknadId: Long,
    val fordeling: String,
    val kriterier: List<String>,
)

data class FordeltRecord(
    val soeknadId: Long,
    val fordeling: String,
    val tidspunkt: Tidspunkt,
)
