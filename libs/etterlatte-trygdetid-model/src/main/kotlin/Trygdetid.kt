package no.nav.etterlatte.libs.common.trygdetid

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDate
import java.time.Period
import java.util.UUID

data class TrygdetidDto(
    val id: UUID,
    val behandlingId: UUID,
    val beregnetTrygdetid: DetaljertBeregnetTrygdetidDto?,
    val trygdetidGrunnlag: List<TrygdetidGrunnlagDto>,
    val opplysninger: GrunnlagOpplysningerDto,
)

data class GrunnlagOpplysningerDto(
    val avdoedDoedsdato: OpplysningsgrunnlagDto?,
    val avdoedFoedselsdato: OpplysningsgrunnlagDto?,
    val avdoedFylteSeksten: OpplysningsgrunnlagDto?,
    val avdoedFyllerSeksti: OpplysningsgrunnlagDto?,
)

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

data class IntBroek(
    val teller: Int,
    val nevner: Int,
) {
    companion object {
        fun fra(broek: Pair<Int?, Int?>): IntBroek? {
            return broek.first?.let { teller ->
                broek.second?.let { nevner ->
                    IntBroek(teller, nevner)
                }
            }
        }
    }
}

data class DetaljertBeregnetTrygdetidResultat(
    val faktiskTrygdetidNorge: FaktiskTrygdetid?,
    val faktiskTrygdetidTeoretisk: FaktiskTrygdetid?,
    val fremtidigTrygdetidNorge: FremtidigTrygdetid?,
    val fremtidigTrygdetidTeoretisk: FremtidigTrygdetid?,
    val samletTrygdetidNorge: Int?,
    val samletTrygdetidTeoretisk: Int?,
    val prorataBroek: IntBroek?,
    val overstyrt: Boolean,
) {
    companion object {
        fun fraSamletTrygdetidNorge(verdi: Int) =
            DetaljertBeregnetTrygdetidResultat(
                faktiskTrygdetidNorge = null,
                faktiskTrygdetidTeoretisk = null,
                fremtidigTrygdetidNorge = null,
                fremtidigTrygdetidTeoretisk = null,
                samletTrygdetidNorge = verdi,
                samletTrygdetidTeoretisk = null,
                prorataBroek = null,
                overstyrt = false,
            )
    }
}

data class FaktiskTrygdetid(
    val periode: Period,
    val antallMaaneder: Long,
)

data class FremtidigTrygdetid(
    val periode: Period,
    val antallMaaneder: Long,
    val opptjeningstidIMaaneder: Long,
    val mindreEnnFireFemtedelerAvOpptjeningstiden: Boolean,
)
