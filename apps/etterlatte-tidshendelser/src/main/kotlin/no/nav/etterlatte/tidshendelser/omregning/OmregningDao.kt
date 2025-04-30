package no.nav.etterlatte.tidshendelser.omregning

import kotliquery.TransactionalSession
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.database.Transactions
import no.nav.etterlatte.libs.database.hent
import no.nav.etterlatte.libs.database.transaction
import javax.sql.DataSource

class OmregningDao(
    private val datasource: DataSource,
) : Transactions<OmregningDao> {
    override fun <R> inTransaction(block: OmregningDao.(TransactionalSession) -> R): R =
        datasource.transaction {
            this.block(it)
        }

    fun hentNyesteKonfigurasjon(): Omregningskonfigurasjon =
        datasource.transaction { tx ->
            with(Databasetabell) {
                tx.hent(
                    "SELECT $ANTALL, $DATO, $SPESIFIKKE_SAKER, $EKSKLUDERTE_SAKER, $KJOERING_ID FROM $TABELLNAVN " +
                        "WHERE $AKTIV=true ORDER BY $OPPRETTET DESC LIMIT 1",
                ) { row ->
                    Omregningskonfigurasjon(
                        antall = row.int(ANTALL),
                        dato = row.localDate(DATO),
                        spesifikkeSaker = row.arrayOrNull<Long>(SPESIFIKKE_SAKER)?.toList()?.map { SakId(it) } ?: emptyList(),
                        ekskluderteSaker = row.arrayOrNull<Long>(EKSKLUDERTE_SAKER)?.toList()?.map { SakId(it) } ?: emptyList(),
                        kjoeringId = row.stringOrNull(KJOERING_ID),
                    )
                }
            } ?: throw IllegalStateException("Det finnes ingen omregningskonfigurasjon i databasen som er aktiv")
        }

    internal object Databasetabell {
        const val TABELLNAVN = "omregningskonfigurasjon"
        const val ANTALL = "antall"
        const val DATO = "dato"
        const val SPESIFIKKE_SAKER = "spesifikke_saker"
        const val EKSKLUDERTE_SAKER = "ekskluderte_saker"
        const val AKTIV = "aktiv"
        const val OPPRETTET = "opprettet"
        const val KJOERING_ID = "kjoering_id"
    }
}
