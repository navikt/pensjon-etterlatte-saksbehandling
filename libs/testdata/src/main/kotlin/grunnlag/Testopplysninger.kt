package grunnlag

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.PeriodisertOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SaksbehandlerMedlemskapsperiode
import no.nav.etterlatte.libs.common.person.UtenlandsoppholdOpplysninger
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.OppholdUtlandType
import no.nav.etterlatte.libs.common.toJsonNode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

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

val medlemskap = listOf(
    PeriodisertOpplysning(
        id = UUID.randomUUID(),
        kilde = Grunnlagsopplysning.Saksbehandler("zid122", Instant.now()),
        fom = YearMonth.of(2021, 1),
        tom = YearMonth.of(2022, 1),
        verdi = SaksbehandlerMedlemskapsperiode(
            periodeType = PeriodeType.DAGPENGER,
            id = UUID.randomUUID().toString(),
            kilde = Grunnlagsopplysning.Saksbehandler("zid122", Instant.now()),
            fraDato = LocalDate.of(2021, 1, 1),
            tilDato = LocalDate.of(2022, 1, 1),
            stillingsprosent = null,
            arbeidsgiver = null,
            begrunnelse = "Sykdom",
            oppgittKilde = "NAV"
        ).toJsonNode()
    ),
    PeriodisertOpplysning(
        id = UUID.randomUUID(),
        kilde = Grunnlagsopplysning.Saksbehandler("zid122", Instant.now()),
        fom = YearMonth.of(2021, 1),
        tom = YearMonth.of(2022, 1),
        verdi = SaksbehandlerMedlemskapsperiode(
            periodeType = PeriodeType.ARBEIDSPERIODE,
            id = UUID.randomUUID().toString(),
            kilde = Grunnlagsopplysning.Saksbehandler("zid122", Instant.now()),
            fraDato = LocalDate.of(2021, 1, 1),
            tilDato = LocalDate.of(2022, 1, 1),
            stillingsprosent = "70.0",
            arbeidsgiver = null,
            begrunnelse = "Annen jobb",
            oppgittKilde = "NAV"
        ).toJsonNode()
    )
)