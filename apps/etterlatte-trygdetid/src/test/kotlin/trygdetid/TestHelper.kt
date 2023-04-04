package trygdetid

import no.nav.etterlatte.token.Saksbehandler
import no.nav.etterlatte.trygdetid.BeregnetTrygdetid
import no.nav.etterlatte.trygdetid.GrunnlagOpplysninger
import no.nav.etterlatte.trygdetid.Trygdetid
import no.nav.etterlatte.trygdetid.TrygdetidGrunnlag
import no.nav.etterlatte.trygdetid.TrygdetidPeriode
import no.nav.etterlatte.trygdetid.TrygdetidType
import java.time.LocalDate
import java.util.*
import java.util.UUID.randomUUID

val saksbehandler = Saksbehandler("token", "ident", null)

fun trygdetid(behandlingId: UUID = randomUUID(), beregnetTrygdetid: BeregnetTrygdetid? = null) = Trygdetid(
    id = randomUUID(),
    behandlingId = behandlingId,
    trygdetidGrunnlag = emptyList(),
    beregnetTrygdetid = beregnetTrygdetid,
    opplysninger = GrunnlagOpplysninger(LocalDate.now(), LocalDate.now())
)

fun trygdetidGrunnlag(trygdetidId: UUID) = TrygdetidGrunnlag(
    id = randomUUID(),
    type = TrygdetidType.NASJONAL,
    bosted = "Norge",
    periode = TrygdetidPeriode(
        fra = LocalDate.of(2023, 1, 1),
        til = LocalDate.of(2023, 2, 1)
    ),
    trygdetid = 10,
    kilde = "pdl"
)

fun beregnetTrygdetid(nasjonal: Int = 0, fremtidig: Int = 0, total: Int = 0) = BeregnetTrygdetid(
    nasjonal = nasjonal,
    fremtidig = fremtidig,
    total = total
)