package trygdetid

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.token.Saksbehandler
import no.nav.etterlatte.trygdetid.BeregnetTrygdetid
import no.nav.etterlatte.trygdetid.BeregnetTrygdetidGrunnlag
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
    beregnetTrygdetid: BeregnetTrygdetid? = null,
    trygdetidGrunnlag: List<TrygdetidGrunnlag> = emptyList()
) = Trygdetid(
    id = randomUUID(),
    behandlingId = behandlingId,
    trygdetidGrunnlag = trygdetidGrunnlag,
    beregnetTrygdetid = beregnetTrygdetid,
    opplysninger = emptyList()
)

fun trygdetidGrunnlag(beregnetTrygdetidGrunnlag: BeregnetTrygdetidGrunnlag? = null) = TrygdetidGrunnlag(
    id = randomUUID(),
    type = TrygdetidType.NASJONAL,
    bosted = "Norge",
    periode = TrygdetidPeriode(
        fra = LocalDate.of(2023, 1, 1),
        til = LocalDate.of(2023, 2, 1)
    ),
    beregnetTrygdetid = beregnetTrygdetidGrunnlag,
    kilde = "pdl"
)

fun beregnetTrygdetid(total: Int = 0, tidspunkt: Tidspunkt = Tidspunkt.now()) =
    BeregnetTrygdetid(
        verdi = total,
        tidspunkt = tidspunkt,
        regelResultat = "".toJsonNode()
    )

fun beregnetTrygdetidGrunnlag() = BeregnetTrygdetidGrunnlag(
    verdi = Period.parse("P2Y2D"),
    tidspunkt = Tidspunkt.now(),
    regelResultat = "".toJsonNode()
)