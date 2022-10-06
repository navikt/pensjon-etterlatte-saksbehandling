package grunnlag

import no.nav.etterlatte.libs.common.grunnlag.PeriodisertOpplysning
import no.nav.etterlatte.libs.common.person.UtenlandsoppholdOpplysninger
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.OppholdUtlandType
import no.nav.etterlatte.libs.common.toJsonNode
import java.time.YearMonth

val utenlandsopphold = listOf(
    PeriodisertOpplysning(
        id = statiskUuid,
        kilde = kilde,
        verdi = UtenlandsoppholdOpplysninger(
            harHattUtenlandsopphold = JaNeiVetIkke.JA,
            land = "Danmark",
            oppholdsType = listOf(OppholdUtlandType.ARBEIDET),
            medlemFolketrygd = JaNeiVetIkke.JA,
            pensjonsutbetaling = null
        ).toJsonNode(),
        fom = YearMonth.of(2010, 1),
        tom = YearMonth.of(2022, 1)
    ),
    PeriodisertOpplysning(
        statiskUuid,
        kilde,
        UtenlandsoppholdOpplysninger(
            harHattUtenlandsopphold = JaNeiVetIkke.JA,
            land = "Costa Rica",
            oppholdsType = listOf(OppholdUtlandType.ARBEIDET),
            medlemFolketrygd = JaNeiVetIkke.NEI,
            pensjonsutbetaling = null
        ).toJsonNode(),
        fom = YearMonth.of(2000, 1),
        tom = YearMonth.of(2007, 1)
    )
)