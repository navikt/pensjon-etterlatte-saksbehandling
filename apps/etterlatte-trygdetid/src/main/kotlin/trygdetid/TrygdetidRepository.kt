package no.nav.etterlatte.trygdetid

import no.nav.etterlatte.trygdetid.config.InMemoryDs
import no.nav.etterlatte.trygdetid.config.InMemoryDs.TrygdetidGrunnlagTable.bosted
import no.nav.etterlatte.trygdetid.config.InMemoryDs.TrygdetidGrunnlagTable.periodeFra
import no.nav.etterlatte.trygdetid.config.InMemoryDs.TrygdetidGrunnlagTable.periodeTil
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

class TrygdetidRepository(private val dataSource: InMemoryDs) {

    fun hentTrygdetid(): Trygdetid {
        return transaction {
            val trygdetidGrunnlag = dataSource.trygdetidTable.selectAll().map {
                toTrygdetidGrunnlag(it)
            }
            Trygdetid(trygdetidGrunnlag)
        }
    }

    private fun toTrygdetidGrunnlag(row: ResultRow): TrygdetidGrunnlag {
        return TrygdetidGrunnlag(
            bosted = row[bosted].toString(),
            periodeFra = row[periodeFra].toString(),
            periodeTil = row[periodeTil].toString()
        )
    }

    fun lagreTrygdetidGrunnlag(trygdetidGrunnlag: TrygdetidGrunnlag) {
        transaction {
            dataSource.trygdetidTable.insert {
                fromTrygdetidGrunnlag(trygdetidGrunnlag, it)
            }
        }
    }

    private fun fromTrygdetidGrunnlag(trygdetid: TrygdetidGrunnlag, it: InsertStatement<Number>) {
        it[bosted] = trygdetid.bosted
        it[periodeFra] = DateTime.parse(trygdetid.periodeFra)
        it[periodeTil] = DateTime.parse(trygdetid.periodeTil)
    }
}