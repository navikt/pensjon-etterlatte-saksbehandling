package trygdetid

import no.nav.etterlatte.trygdetid.BeregnetTrygdetid
import no.nav.etterlatte.trygdetid.TrygdetidGrunnlag
import no.nav.etterlatte.trygdetid.TrygdetidPeriode
import no.nav.etterlatte.trygdetid.TrygdetidType
import java.time.LocalDate
import java.util.*

fun trygdetidGrunnlag(trygdetidId: UUID) = TrygdetidGrunnlag(
    id = UUID.randomUUID(),
    trygdetidId = trygdetidId,
    type = TrygdetidType.NASJONAL,
    bosted = "Norge",
    periode = TrygdetidPeriode(
        fra = LocalDate.of(2023, 1, 1),
        til = LocalDate.of(2023, 2, 1)
    ),
    kilde = "pdl"
)

fun beregnetTrygdetid(nasjonal: Int = 0, fremtidig: Int = 0, total: Int = 0) = BeregnetTrygdetid(
    nasjonal = nasjonal,
    fremtidig = fremtidig,
    total = total
)