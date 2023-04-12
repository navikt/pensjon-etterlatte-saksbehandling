package no.nav.etterlatte.libs.common.trygdetid

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDate
import java.util.*

data class TrygdetidDto(
    val id: UUID,
    val behandlingId: UUID,
    val beregnetTrygdetid: BeregnetTrygdetidDto?,
    val trygdetidGrunnlag: List<TrygdetidGrunnlagDto>,
    val opplysninger: GrunnlagOpplysningerDto
)

data class GrunnlagOpplysningerDto(
    val avdoedDoedsdato: LocalDate?,
    val avdoedFoedselsdato: LocalDate?
)

data class BeregnetTrygdetidDto(
    val nasjonal: Int,
    val fremtidig: Int,
    val total: Int,
    val tidspunkt: Tidspunkt?
)

data class TrygdetidGrunnlagDto(
    val id: UUID?,
    val type: String,
    val bosted: String,
    val periodeFra: LocalDate,
    val periodeTil: LocalDate,
    val trygdetid: Int,
    val kilde: String
)