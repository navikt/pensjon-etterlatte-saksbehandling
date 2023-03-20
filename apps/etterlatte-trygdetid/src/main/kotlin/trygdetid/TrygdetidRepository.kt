package no.nav.etterlatte.trygdetid

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.trygdetid.config.InMemoryDs
import no.nav.etterlatte.trygdetid.config.InMemoryDs.TrygdetidGrunnlagTable.bosted
import no.nav.etterlatte.trygdetid.config.InMemoryDs.TrygdetidGrunnlagTable.kilde
import no.nav.etterlatte.trygdetid.config.InMemoryDs.TrygdetidGrunnlagTable.periodeFra
import no.nav.etterlatte.trygdetid.config.InMemoryDs.TrygdetidGrunnlagTable.periodeTil
import no.nav.etterlatte.trygdetid.config.InMemoryDs.TrygdetidGrunnlagTable.trygdetidType
import no.nav.etterlatte.trygdetid.config.InMemoryDs.TrygdetidTable.behandlingsId
import no.nav.etterlatte.trygdetid.config.InMemoryDs.TrygdetidTable.fremtidigTrygdetid
import no.nav.etterlatte.trygdetid.config.InMemoryDs.TrygdetidTable.nasjonalTrygdetid
import no.nav.etterlatte.trygdetid.config.InMemoryDs.TrygdetidTable.opprettet
import no.nav.etterlatte.trygdetid.config.InMemoryDs.TrygdetidTable.totalTrygdetid
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.*

class TrygdetidRepository(private val dataSource: InMemoryDs) {

    fun hentTrygdetid(behandlingsId: UUID): Trygdetid? {
        return transaction {
            val result = dataSource.trygdetidTable.select(dataSource.trygdetidTable.behandlingsId eq behandlingsId)
            result.firstOrNull()?.let {
                val trygdetidId = it[dataSource.trygdetidTable.id]
                val trygdetidgrunnlag = hentTrygdetidGrunnlag(trygdetidId)
                it.toTrygdetid(trygdetidgrunnlag)
            }
        }
    }

    private fun hentTrygdtidNotNull(behandlingsId: UUID) =
        hentTrygdetid(behandlingsId)
            ?: throw RuntimeException("Fant ikke trygdetid for ${InMemoryDs.TrygdetidTable.behandlingsId}")

    private fun ResultRow.toTrygdetid(trygdetidGrunnlag: List<TrygdetidGrunnlag>): Trygdetid {
        return Trygdetid(
            id = this[InMemoryDs.TrygdetidTable.id],
            behandlingId = this[behandlingsId],
            opprettet = Tidspunkt(this[opprettet]),
            beregnetTrygdetid = this[nasjonalTrygdetid]?.let {
                BeregnetTrygdetid(
                    nasjonal = this[nasjonalTrygdetid]!!,
                    fremtidig = this[fremtidigTrygdetid]!!,
                    total = this[totalTrygdetid]!!
                )
            },
            trygdetidGrunnlag = trygdetidGrunnlag
        )
    }

    private fun hentTrygdetidGrunnlag(trygdetidId: UUID): List<TrygdetidGrunnlag> {
        return transaction {
            dataSource.trygdetidGrunnlagTable.select(
                dataSource.trygdetidGrunnlagTable.trygdetidId eq trygdetidId
            ).map {
                it.toTrygdetidGrunnlag()
            }
        }
    }

    private fun ResultRow.toTrygdetidGrunnlag(): TrygdetidGrunnlag {
        return TrygdetidGrunnlag(
            id = this[InMemoryDs.TrygdetidGrunnlagTable.id],
            bosted = this[bosted],
            type = TrygdetidType.valueOf(this[trygdetidType]),
            periode = TrygdetidPeriode(fra = this[periodeFra], til = this[periodeTil]),
            kilde = this[kilde]
        )
    }

    fun opprettTrygdetid(behandlingsId: UUID): Trygdetid {
        transaction {
            dataSource.trygdetidTable.insert {
                it[this.id] = UUID.randomUUID()
                it[this.behandlingsId] = behandlingsId
                it[this.opprettet] = Instant.now() // TODO denne skal hentes fra tidspunkt
            }
        }
        return hentTrygdtidNotNull(behandlingsId)
    }

    fun lagreTrygdetidGrunnlag(behandlingsId: UUID, trygdetidGrunnlag: TrygdetidGrunnlag): Trygdetid {
        val trygdetid = hentTrygdtidNotNull(behandlingsId)

        transaction {
            dataSource.trygdetidGrunnlagTable.insert {
                it[id] = trygdetidGrunnlag.id
                it[trygdetidId] = trygdetid.id
                it[trygdetidType] = trygdetidGrunnlag.type.name
                it[bosted] = trygdetidGrunnlag.bosted
                it[periodeFra] = trygdetidGrunnlag.periode.fra
                it[periodeTil] = trygdetidGrunnlag.periode.til
                it[kilde] = trygdetidGrunnlag.kilde
            }
        }
        return hentTrygdtidNotNull(behandlingsId)
    }

    fun lagreBeregnetTrygdetid(behandlingsId: UUID, beregnetTrygdetid: BeregnetTrygdetid): Trygdetid {
        transaction {
            dataSource.trygdetidTable.update({
                dataSource.trygdetidTable.behandlingsId eq behandlingsId
            }) {
                it[nasjonalTrygdetid] = beregnetTrygdetid.nasjonal
                it[fremtidigTrygdetid] = beregnetTrygdetid.fremtidig
                it[this.totalTrygdetid] = beregnetTrygdetid.total
            }
        }
        return hentTrygdtidNotNull(behandlingsId)
    }
}