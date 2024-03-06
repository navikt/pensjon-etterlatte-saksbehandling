package no.nav.etterlatte.migrering.vent

import kotliquery.TransactionalSession
import no.nav.etterlatte.libs.database.Transactions
import no.nav.etterlatte.libs.database.hent
import no.nav.etterlatte.libs.database.oppdater
import no.nav.etterlatte.libs.database.transaction
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringKjoringVariant
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

class VentRepository(val dataSource: DataSource) : Transactions<VentRepository> {
    override fun <R> inTransaction(block: VentRepository.(TransactionalSession) -> R): R =
        dataSource.transaction {
            this.block(it)
        }

    fun hentSakerSomSkalAvVent(tx: TransactionalSession? = null) =
        tx.session {
            with(Databasetabell) {
                hent(
                    "SELECT $ID, $DATO, $OPPGAVER, $KJOERING FROM $TABELLNAVN WHERE $HAANDTERT=FALSE",
                    mapOf(),
                ) {
                    SkalAvVentDTO(
                        id = it.uuid(ID),
                        dato = it.localDate(DATO),
                        kjoringVariant = MigreringKjoringVariant.valueOf(it.string(KJOERING)),
                        oppgaver =
                            it.string(OPPGAVER).split(";")
                                .filter { id -> id.isNotEmpty() },
                    )
                }
            }
        }

    fun settSakerAvVent(
        avVent: SkalAvVentDTO,
        tx: TransactionalSession? = null,
    ) = tx.session {
        with(Databasetabell) {
            oppdater(
                query = "UPDATE $TABELLNAVN SET $HAANDTERT=true WHERE $ID=:$ID",
                params = mapOf(ID to avVent.id),
                loggtekst = "Markerte ${avVent.id} som tatt av vent",
            )
        }
    }

    internal object Databasetabell {
        const val TABELLNAVN = "ta_av_vent"
        const val ID = "id"
        const val HAANDTERT = "haandtert"
        const val KJOERING = "kjoering"
        const val DATO = "dato"
        const val OPPGAVER = "oppgaver"
    }
}

data class SkalAvVentDTO(
    val id: UUID,
    val dato: LocalDate,
    val kjoringVariant: MigreringKjoringVariant,
    val oppgaver: List<String>,
)
