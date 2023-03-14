package no.nav.etterlatte.trygdetid

import no.nav.etterlatte.trygdetid.config.InMemoryDs
import no.nav.etterlatte.trygdetid.config.InMemoryDs.TrygdetidGrunnlagTable.bosted
import no.nav.etterlatte.trygdetid.config.InMemoryDs.TrygdetidGrunnlagTable.periodeFra
import no.nav.etterlatte.trygdetid.config.InMemoryDs.TrygdetidGrunnlagTable.periodeTil
import no.nav.etterlatte.trygdetid.config.InMemoryDs.TrygdetidTable.behandlingsId
import no.nav.etterlatte.trygdetid.config.InMemoryDs.TrygdetidTable.oppsummertTrygdetid
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.util.*

class TrygdetidRepository(private val dataSource: InMemoryDs) {

    fun hentTrygdetid(behandlingsId: UUID): Trygdetid? {
        return transaction {
            val result = dataSource.trygdetidTable.select(dataSource.trygdetidTable.behandlingsId eq behandlingsId)
            result.firstOrNull()?.let {
                val trygdetidgrunnlag = hentTrygdetidGrunnlag(behandlingsId)
                it.toTrygdetid(trygdetidgrunnlag)
            }
        }
    }

    private fun hentTrygdtidNotNull(behandlingsId: UUID) =
        hentTrygdetid(behandlingsId)
            ?: throw RuntimeException("Fant ikke trygdetid for ${InMemoryDs.TrygdetidTable.behandlingsId}")

    private fun ResultRow.toTrygdetid(trygdetidGrunnlag: List<TrygdetidGrunnlag>): Trygdetid {
        return Trygdetid(
            behandlingsId = this[behandlingsId],
            oppsummertTrygdetid = this[oppsummertTrygdetid],
            grunnlag = trygdetidGrunnlag
        )
    }

    private fun hentTrygdetidGrunnlag(behandlingsId: UUID): List<TrygdetidGrunnlag> {
        return transaction {
            dataSource.trygdetidGrunnlagTable.select(
                dataSource.trygdetidGrunnlagTable.behandlingsId eq behandlingsId
            ).map {
                it.toTrygdetidGrunnlag()
            }
        }
    }

    private fun ResultRow.toTrygdetidGrunnlag(): TrygdetidGrunnlag {
        return TrygdetidGrunnlag(
            bosted = this[bosted],
            periodeFra = this[periodeFra].toString(),
            periodeTil = this[periodeTil].toString()
        )
    }

    fun opprettTrygdetid(behandlingsId: UUID): Trygdetid {
        transaction {
            dataSource.trygdetidTable.insert {
                it[this.behandlingsId] = behandlingsId
            }
        }
        return hentTrygdtidNotNull(behandlingsId)
    }

    fun lagreTrygdetidGrunnlag(behandlingsId: UUID, trygdetidGrunnlag: TrygdetidGrunnlag): Trygdetid {
        transaction {
            dataSource.trygdetidGrunnlagTable.insert {
                it[this.behandlingsId] = behandlingsId
                it[bosted] = trygdetidGrunnlag.bosted
                it[periodeFra] = LocalDate.parse(trygdetidGrunnlag.periodeFra)
                it[periodeTil] = LocalDate.parse(trygdetidGrunnlag.periodeTil)
            }
        }
        return hentTrygdtidNotNull(behandlingsId)
    }
}