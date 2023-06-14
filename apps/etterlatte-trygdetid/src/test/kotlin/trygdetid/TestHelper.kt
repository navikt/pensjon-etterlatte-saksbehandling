package trygdetid

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.token.Saksbehandler
import no.nav.etterlatte.trygdetid.BeregnetTrygdetid
import no.nav.etterlatte.trygdetid.BeregnetTrygdetidGrunnlag
import no.nav.etterlatte.trygdetid.LandNormalisert
import no.nav.etterlatte.trygdetid.Opplysningsgrunnlag
import no.nav.etterlatte.trygdetid.Trygdetid
import no.nav.etterlatte.trygdetid.TrygdetidGrunnlag
import no.nav.etterlatte.trygdetid.TrygdetidPeriode
import no.nav.etterlatte.trygdetid.TrygdetidType
import java.time.LocalDate
import java.time.Period
import java.util.*
import java.util.UUID.randomUUID

val saksbehandler = Saksbehandler("token", "ident", null)

fun trygdetid(
    behandlingId: UUID = randomUUID(),
    sakId: Long = 1,
    beregnetTrygdetid: BeregnetTrygdetid? = null,
    trygdetidGrunnlag: List<TrygdetidGrunnlag> = emptyList(),
    opplysninger: List<Opplysningsgrunnlag> = emptyList()
) = Trygdetid(
    id = randomUUID(),
    sakId = sakId,
    behandlingId = behandlingId,
    trygdetidGrunnlag = trygdetidGrunnlag,
    opplysninger = opplysninger,
    beregnetTrygdetid = beregnetTrygdetid
)

fun trygdetidGrunnlag(
    beregnetTrygdetidGrunnlag: BeregnetTrygdetidGrunnlag? = beregnetTrygdetidGrunnlag(),
    periode: TrygdetidPeriode = TrygdetidPeriode(
        fra = LocalDate.of(2023, 1, 1),
        til = LocalDate.of(2023, 2, 1)
    ),
    begrunnelse: String? = null,
    poengInnAar: Boolean = false,
    poengUtAar: Boolean = false,
    prorata: Boolean = false
) = TrygdetidGrunnlag(
    id = randomUUID(),
    type = TrygdetidType.FAKTISK,
    bosted = LandNormalisert.NORGE.isoCode,
    periode = periode,
    beregnetTrygdetid = beregnetTrygdetidGrunnlag,
    kilde = Grunnlagsopplysning.Saksbehandler(ident = "Z123", tidspunkt = Tidspunkt.now()),
    begrunnelse = begrunnelse,
    poengUtAar = poengUtAar,
    poengInnAar = poengInnAar,
    prorata = prorata
)

fun beregnetTrygdetid(total: Int = 0, tidspunkt: Tidspunkt = Tidspunkt.now()) =
    BeregnetTrygdetid(
        verdi = total,
        tidspunkt = tidspunkt,
        regelResultat = "".toJsonNode()
    )

fun beregnetTrygdetidGrunnlag(verdi: Period = Period.parse("P2Y2D")) = BeregnetTrygdetidGrunnlag(
    verdi = verdi,
    tidspunkt = Tidspunkt.now(),
    regelResultat = "".toJsonNode()
)