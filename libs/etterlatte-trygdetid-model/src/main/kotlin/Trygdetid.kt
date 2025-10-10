package no.nav.etterlatte.libs.common.trygdetid

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDate
import java.time.Period
import java.util.UUID

data class TrygdetidDto(
    val id: UUID,
    val ident: String,
    val behandlingId: UUID,
    val beregnetTrygdetid: DetaljertBeregnetTrygdetidDto?,
    val trygdetidGrunnlag: List<TrygdetidGrunnlagDto>,
    val opplysninger: GrunnlagOpplysningerDto,
    val overstyrtNorskPoengaar: Int?,
    val opplysningerDifferanse: OpplysningerDifferanse,
    val begrunnelse: String?,
)

const val UKJENT_AVDOED = "UKJENT_AVDOED"

data class TrygdetidOverstyringDto(
    val id: UUID,
    val behandlingId: UUID,
    val overstyrtNorskPoengaar: Int?,
)

data class TrygdetidYrkesskadeDto(
    val id: UUID,
    val behandlingId: UUID,
    val yrkesskade: Boolean,
)

data class TrygdetidBegrunnelseDto(
    val id: UUID,
    val behandlingId: UUID,
    val begrunnelse: String?,
)

data class GrunnlagOpplysningerDto(
    val avdoedDoedsdato: OpplysningsgrunnlagDto?,
    val avdoedFoedselsdato: OpplysningsgrunnlagDto?,
    val avdoedFylteSeksten: OpplysningsgrunnlagDto?,
    val avdoedFyllerSeksti: OpplysningsgrunnlagDto?,
) {
    companion object {
        fun tomt() =
            GrunnlagOpplysningerDto(
                null,
                null,
                null,
                null,
            )
    }
}

data class OpplysningsgrunnlagDto(
    val opplysning: JsonNode,
    val kilde: OpplysningkildeDto,
)

data class OpplysningkildeDto(
    val type: String,
    val tidspunkt: String,
)

data class DetaljertBeregnetTrygdetidDto(
    val resultat: DetaljertBeregnetTrygdetidResultat,
    val tidspunkt: Tidspunkt,
)

data class TrygdetidGrunnlagDto(
    val id: UUID?,
    val type: String,
    val bosted: String,
    val periodeFra: LocalDate,
    val periodeTil: LocalDate,
    val kilde: TrygdetidGrunnlagKildeDto?,
    val beregnet: BeregnetTrygdetidGrunnlagDto?,
    val begrunnelse: String?,
    val poengInnAar: Boolean,
    val poengUtAar: Boolean,
    val prorata: Boolean,
)

data class TrygdetidGrunnlagKildeDto(
    val tidspunkt: String,
    val ident: String,
)

data class BeregnetTrygdetidGrunnlagDto(
    val dager: Int,
    val maaneder: Int,
    val aar: Int,
)

data class MigreringOverstyringDto(
    val ident: String,
    val detaljertBeregnetTrygdetidResultat: DetaljertBeregnetTrygdetidResultat,
)

data class StatusOppdatertDto(
    val statusOppdatert: Boolean,
)

data class DetaljertBeregnetTrygdetidResultat(
    val faktiskTrygdetidNorge: FaktiskTrygdetid?,
    val faktiskTrygdetidTeoretisk: FaktiskTrygdetid?,
    val fremtidigTrygdetidNorge: FremtidigTrygdetid?,
    val fremtidigTrygdetidTeoretisk: FremtidigTrygdetid?,
    val samletTrygdetidNorge: Int?,
    val samletTrygdetidTeoretisk: Int?,
    val prorataBroek: IntBroek?,
    val overstyrt: Boolean,
    val yrkesskade: Boolean,
    val beregnetSamletTrygdetidNorge: Int?,
    val overstyrtBegrunnelse: String? = null,
) {
    companion object {
        fun fraSamletTrygdetidProrata(
            anvendtTrygdetid: Int,
            prorataBroek: IntBroek?,
        ) = DetaljertBeregnetTrygdetidResultat(
            faktiskTrygdetidNorge = null,
            faktiskTrygdetidTeoretisk = null,
            fremtidigTrygdetidNorge = null,
            fremtidigTrygdetidTeoretisk = null,
            samletTrygdetidNorge = null,
            samletTrygdetidTeoretisk = anvendtTrygdetid,
            prorataBroek = prorataBroek,
            overstyrt = true,
            yrkesskade = false,
            beregnetSamletTrygdetidNorge = null,
        )
    }
}

data class FaktiskTrygdetid(
    val periode: Period,
    val antallMaaneder: Long,
) {
    fun avrundet(): FaktiskTrygdetid {
        val avrundetPeriode = this.periode.justertForDager()
        return this.copy(periode = avrundetPeriode, antallMaaneder = avrundetPeriode.toTotalMonths())
    }
}

fun Period.justertForDager(): Period {
    val antallMaaneder =
        if (this.normalized().days > 0) {
            this.toTotalMonths() + 1L
        } else {
            this.toTotalMonths()
        }
    return Period.ofMonths(antallMaaneder.toInt()).normalized()
}

data class FremtidigTrygdetid(
    val periode: Period,
    val antallMaaneder: Long,
    val opptjeningstidIMaaneder: Long,
    val mindreEnnFireFemtedelerAvOpptjeningstiden: Boolean,
) {
    fun avrundet(): FremtidigTrygdetid {
        val avrundetPeriode = this.periode.justertForDager()
        return this.copy(periode = avrundetPeriode, antallMaaneder = avrundetPeriode.toTotalMonths())
    }
}

data class OpplysningerDifferanse(
    val differanse: Boolean,
    val oppdaterteGrunnlagsopplysninger: GrunnlagOpplysningerDto,
)
