package no.nav.etterlatte.trygdetid

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import java.time.LocalDate
import java.time.MonthDay
import java.time.Period
import java.util.UUID
import java.util.UUID.randomUUID

data class Trygdetid(
    val id: UUID = randomUUID(),
    val sakId: Long,
    val behandlingId: UUID,
    val trygdetidGrunnlag: List<TrygdetidGrunnlag> = emptyList(),
    val opplysninger: List<Opplysningsgrunnlag> = emptyList(),
    val beregnetTrygdetid: DetaljertBeregnetTrygdetid? = null,
    val overstyrtNorskPoengaar: Int? = null,
) {
    fun leggTilEllerOppdaterTrygdetidGrunnlag(nyttTrygdetidGrunnlag: TrygdetidGrunnlag): Trygdetid {
        val normalisertNyttTrygdetidGrunnlag = listOf(nyttTrygdetidGrunnlag).normaliser().first()

        trygdetidGrunnlag.normaliser().filter {
            it.id != normalisertNyttTrygdetidGrunnlag.id
        }.find { it.periode.overlapperMed(normalisertNyttTrygdetidGrunnlag.periode) }?.let {
            throw OverlappendePeriodeException("Trygdetidsperioder kan ikke være overlappende")
        }

        val oppdatertGrunnlagListe =
            trygdetidGrunnlag.toMutableList().apply {
                removeAll { it.id == nyttTrygdetidGrunnlag.id }

                add(nyttTrygdetidGrunnlag)
            }

        return this.copy(trygdetidGrunnlag = oppdatertGrunnlagListe)
    }

    fun slettTrygdetidGrunnlag(trygdetidGrunnlagId: UUID): Trygdetid =
        this.copy(
            trygdetidGrunnlag = this.trygdetidGrunnlag.filter { it.id != trygdetidGrunnlagId },
        )

    fun oppdaterBeregnetTrygdetid(beregnetTrygdetid: DetaljertBeregnetTrygdetid): Trygdetid {
        return this.copy(beregnetTrygdetid = beregnetTrygdetid)
    }

    fun nullstillBeregnetTrygdetid(): Trygdetid {
        return this.copy(beregnetTrygdetid = null)
    }

    // Må skrives om når vi gjør om til å støtte utenlands og poeng i inn/ut år (relatert til prorata)
    fun isYrkesskade() = this.beregnetTrygdetid?.regelResultat?.toString()?.contains("yrkesskade", ignoreCase = true)
}

data class DetaljertBeregnetTrygdetid(
    val resultat: DetaljertBeregnetTrygdetidResultat,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode,
)

data class Opplysningsgrunnlag(
    val id: UUID,
    val type: TrygdetidOpplysningType,
    val opplysning: JsonNode,
    val kilde: Grunnlagsopplysning.Kilde,
) {
    companion object {
        fun ny(
            type: TrygdetidOpplysningType,
            kilde: Grunnlagsopplysning.Kilde?,
            verdi: LocalDate?,
        ): Opplysningsgrunnlag =
            Opplysningsgrunnlag(
                id = randomUUID(),
                type = type,
                opplysning = verdi?.toJsonNode() ?: throw Exception("Mangler opplysning"),
                kilde = kilde ?: throw Exception("Mangler kilde"),
            )
    }
}

enum class TrygdetidOpplysningType {
    FOEDSELSDATO,
    DOEDSDATO,
    FYLT_16,
    FYLLER_66,
}

data class TrygdetidGrunnlag(
    val id: UUID,
    val type: TrygdetidType,
    val bosted: String,
    val periode: TrygdetidPeriode,
    val kilde: Grunnlagsopplysning.Kilde,
    val beregnetTrygdetid: BeregnetTrygdetidGrunnlag? = null,
    val begrunnelse: String?,
    val poengInnAar: Boolean,
    val poengUtAar: Boolean,
    val prorata: Boolean,
) {
    fun oppdaterBeregnetTrygdetid(beregnetTrygdetid: BeregnetTrygdetidGrunnlag?): TrygdetidGrunnlag {
        return this.copy(beregnetTrygdetid = beregnetTrygdetid)
    }

    fun erNasjonal() = this.bosted == LandNormalisert.NORGE.isoCode
}

data class BeregnetTrygdetidGrunnlag(val verdi: Period, val tidspunkt: Tidspunkt, val regelResultat: JsonNode)

data class TrygdetidPeriode(
    val fra: LocalDate,
    val til: LocalDate,
) {
    init {
        require(fra.isBefore(til) || fra.isEqual(til)) { "Ugyldig periode, fra må være før eller lik til" }
    }

    fun overlapperMed(other: TrygdetidPeriode): Boolean {
        return this.fra.isBefore(other.til) && other.fra.isBefore(this.til)
    }
}

fun List<TrygdetidGrunnlag>.normaliser() =
    this.mapIndexed { idx, trygdetidGrunnlag ->
        var fra = trygdetidGrunnlag.periode.fra
        var til = trygdetidGrunnlag.periode.til

        if (trygdetidGrunnlag.poengInnAar) {
            fra = fra.with(MonthDay.of(1, 1))
        }

        if (trygdetidGrunnlag.poengUtAar) {
            til = til.with(MonthDay.of(12, 31))
        }

        // Håndtere at den forrige var et ut år - og at dette hadde en fra i samme år
        if (idx > 0) {
            val prev = this[idx - 1]

            if (prev.poengUtAar && prev.periode.til.year == fra.year) {
                fra = LocalDate.of(prev.periode.til.year + 1, 1, 1)
            }
        }

        // Håndtere at den neste var et inn år - og at dette hadde en til i samme år
        if (idx < this.size - 2) {
            val next = this[idx + 1]

            if (next.poengInnAar && next.periode.til.year == fra.year) {
                til = LocalDate.of(next.periode.til.year - 1, 12, 31)
            }
        }

        trygdetidGrunnlag.copy(periode = TrygdetidPeriode(fra = fra, til = til.plusDays(1)))
    }

class OverlappendePeriodeException(override val message: String) : RuntimeException(message)
