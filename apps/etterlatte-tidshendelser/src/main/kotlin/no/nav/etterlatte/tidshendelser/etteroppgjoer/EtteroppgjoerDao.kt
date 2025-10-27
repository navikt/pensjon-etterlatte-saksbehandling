package no.nav.etterlatte.tidshendelser.etteroppgjoer

import kotliquery.TransactionalSession
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.database.Transactions
import no.nav.etterlatte.libs.database.hent
import no.nav.etterlatte.libs.database.transaction
import javax.sql.DataSource

class EtteroppgjoerDao(
    private val dataSource: DataSource,
) : Transactions<EtteroppgjoerDao> {
    override fun <R> inTransaction(block: EtteroppgjoerDao.(TransactionalSession) -> R): R = dataSource.transaction { this.block(it) }

    fun hentNyesteKonfigurasjon(): EtteroppgjoerKonfigurasjon =
        dataSource.transaction { tx ->
            with(Databasetabell) {
                tx.hent(
                    "SELECT $INNNTEKTSAAR, $ANTALL, $DATO, $ETTEROPPGJOER_FILTER, $SPESIFIKKE_SAKER, $EKSKLUDERTE_SAKER, $KJOERING_ID " +
                        "FROM $TABELLNAVN " +
                        "WHERE $AKTIV=true ORDER BY $OPPRETTET DESC LIMIT 1",
                ) { row ->
                    EtteroppgjoerKonfigurasjon(
                        inntektsaar = row.int(INNNTEKTSAAR),
                        antall = row.int(ANTALL),
                        dato = row.localDate(DATO),
                        etteroppgjoerFilter = enumValueOf<EtteroppgjoerFilter>(row.string(ETTEROPPGJOER_FILTER)),
                        spesifikkeSaker = row.arrayOrNull<Long>(SPESIFIKKE_SAKER)?.toList()?.map { SakId(it) } ?: emptyList(),
                        ekskluderteSaker = row.arrayOrNull<Long>(EKSKLUDERTE_SAKER)?.toList()?.map { SakId(it) } ?: emptyList(),
                        spesifikkeEnheter = row.arrayOrNull<String>(SPESIFIKKE_ENHETER)?.toList()?.map { it } ?: emptyList(),
                        kjoeringId = row.stringOrNull(KJOERING_ID),
                    )
                } ?: throw IllegalStateException("Det finnes ingen etteroppgjør konfigurasjon i databasen som er aktiv")
            }
        }

    internal object Databasetabell {
        const val TABELLNAVN = "etteroppgjoer_konfigurasjon"
        const val INNNTEKTSAAR = "inntektsaar"
        const val ANTALL = "antall"
        const val DATO = "dato"
        const val ETTEROPPGJOER_FILTER = "etteroppgjoer_filter"
        const val SPESIFIKKE_SAKER = "spesifikke_saker"
        const val EKSKLUDERTE_SAKER = "ekskluderte_saker"
        const val SPESIFIKKE_ENHETER = "spesifikke_enheter"
        const val AKTIV = "aktiv"
        const val OPPRETTET = "opprettet"
        const val KJOERING_ID = "kjoering_id"
    }
}
