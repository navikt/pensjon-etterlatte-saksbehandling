package no.nav.etterlatte.trygdetid

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.GrunnlagOpplysningerDto
import no.nav.etterlatte.libs.common.trygdetid.OpplysningerDifferanse
import no.nav.etterlatte.libs.common.trygdetid.OpplysningkildeDto
import no.nav.etterlatte.libs.common.trygdetid.OpplysningsgrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.land.LandNormalisert
import java.time.LocalDate
import java.time.MonthDay
import java.time.Period
import java.util.UUID
import java.util.UUID.randomUUID

data class Trygdetid(
    val id: UUID = randomUUID(),
    val ident: String,
    val sakId: Long,
    val behandlingId: UUID,
    val trygdetidGrunnlag: List<TrygdetidGrunnlag> = emptyList(),
    val opplysninger: List<Opplysningsgrunnlag> = emptyList(),
    val beregnetTrygdetid: DetaljertBeregnetTrygdetid? = null,
    val overstyrtNorskPoengaar: Int? = null,
    val opplysningerDifferanse: OpplysningerDifferanse? = null,
    val yrkesskade: Boolean,
) {
    fun leggTilEllerOppdaterTrygdetidGrunnlag(nyttTrygdetidGrunnlag: TrygdetidGrunnlag): Trygdetid {
        val normalisertNyttTrygdetidGrunnlag = listOf(nyttTrygdetidGrunnlag).normaliser().first()

        trygdetidGrunnlag
            .normaliser()
            .filter {
                it.id != normalisertNyttTrygdetidGrunnlag.id
            }.find { it.periode.overlapperMed(normalisertNyttTrygdetidGrunnlag.periode) }
            ?.let {
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

    fun oppdaterBeregnetTrygdetid(beregnetTrygdetid: DetaljertBeregnetTrygdetid): Trygdetid =
        this.copy(beregnetTrygdetid = beregnetTrygdetid)

    fun nullstillBeregnetTrygdetid(): Trygdetid = this.copy(beregnetTrygdetid = null)
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
            kilde: Grunnlagsopplysning.Kilde,
            verdi: LocalDate,
        ): Opplysningsgrunnlag =
            Opplysningsgrunnlag(
                id = randomUUID(),
                type = type,
                opplysning = verdi.toJsonNode(),
                kilde = kilde,
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
    fun oppdaterBeregnetTrygdetid(beregnetTrygdetid: BeregnetTrygdetidGrunnlag?): TrygdetidGrunnlag =
        this.copy(beregnetTrygdetid = beregnetTrygdetid)

    fun erNasjonal() = this.bosted == LandNormalisert.NORGE.isoCode
}

data class BeregnetTrygdetidGrunnlag(
    val verdi: Period,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode,
)

data class TrygdetidPeriode(
    val fra: LocalDate,
    val til: LocalDate,
) {
    init {
        if (fra.isAfter(til)) {
            throw UgyldigForespoerselException(
                code = "TRYGDETID_UGYLDIG_PERIODE",
                detail = "Ugyldig periode, fra må være før eller lik til. Fra var $fra, til var $til",
            )
        }
    }

    fun overlapperMed(other: TrygdetidPeriode): Boolean = this.fra.isBefore(other.til) && other.fra.isBefore(this.til)
}

fun List<TrygdetidGrunnlag>.normaliser() =
    this.sortedBy { it.periode.fra }.mapIndexed { idx, trygdetidGrunnlag ->
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

class OverlappendePeriodeException(
    message: String,
) : ForespoerselException(
        status = HttpStatusCode.Conflict.value,
        code = "OVERLAPPENDE_PERIODE_TRYGDETID",
        detail = message,
    )

fun List<Opplysningsgrunnlag>.toDto(): GrunnlagOpplysningerDto =
    GrunnlagOpplysningerDto(
        avdoedFoedselsdato = this.finnOpplysning(TrygdetidOpplysningType.FOEDSELSDATO),
        avdoedDoedsdato = this.finnOpplysning(TrygdetidOpplysningType.DOEDSDATO),
        avdoedFylteSeksten = this.finnOpplysning(TrygdetidOpplysningType.FYLT_16),
        avdoedFyllerSeksti = this.finnOpplysning(TrygdetidOpplysningType.FYLLER_66),
    )

fun List<Opplysningsgrunnlag>.finnOpplysning(type: TrygdetidOpplysningType): OpplysningsgrunnlagDto? =
    this.find { opplysning -> opplysning.type == type }?.toDto()

private fun Opplysningsgrunnlag.toDto(): OpplysningsgrunnlagDto =
    OpplysningsgrunnlagDto(
        opplysning = this.opplysning,
        kilde =
            when (this.kilde) {
                is Grunnlagsopplysning.Pdl ->
                    OpplysningkildeDto(
                        type = this.kilde.type,
                        tidspunkt = this.kilde.tidspunktForInnhenting.toString(),
                    )

                is Grunnlagsopplysning.RegelKilde ->
                    OpplysningkildeDto(
                        type = this.kilde.type,
                        tidspunkt = this.kilde.ts.toString(),
                    )

                else -> throw Exception("Mangler gyldig kilde for opplysning $id")
            },
    )
