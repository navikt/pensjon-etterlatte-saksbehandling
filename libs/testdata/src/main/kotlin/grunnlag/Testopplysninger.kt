package grunnlag

import no.nav.etterlatte.libs.common.arbeidsforhold.AaregAnsettelsesdetaljer
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregAnsettelsesperiode
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregArbeidssted
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregArbeidstaker
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregBruksperiode
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregFraTil
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregIdent
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregKodeBeskrivelse
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregOpplysningspliktig
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregResponse
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
import java.time.LocalDateTime
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

fun arbeidsforholdTestData(stillingsprosent: Double) = listOf(
    PeriodisertOpplysning(
        id = UUID.randomUUID(),
        kilde = Grunnlagsopplysning.AAregisteret(Instant.now()),
        fom = YearMonth.of(2002, 8),
        tom = null,
        verdi = AaregResponse(
            id = 1,
            type = AaregKodeBeskrivelse(kode = "ordinaertArbeidsforhold", beskrivelse = "Ordinært arbeidsforhold"),
            arbeidstaker = AaregArbeidstaker(
                identer = listOf(
                    AaregIdent(
                        type = "AKTORID",
                        ident = "2926321017666",
                        gjeldende = true
                    ),
                    AaregIdent(type = "FOLKEREGISTERIDENT", ident = "03437401844", gjeldende = true)
                )
            ),
            arbeidssted = AaregArbeidssted(
                type = "Underenhet",
                identer = listOf(AaregIdent(type = "ORGANISASJONSNUMMER", ident = "967170232", gjeldende = null))
            ),
            opplysningspliktig = AaregOpplysningspliktig(
                type = "Hovedenhet",
                identer = listOf(AaregIdent(type = "ORGANISASJONSNUMMER", ident = "928497704", gjeldende = null))
            ),
            ansettelsesperiode = AaregAnsettelsesperiode(
                startdato = LocalDate.parse("2002-08-23"),
                sluttdato = null
            ),
            ansettelsesdetaljer = listOf(
                AaregAnsettelsesdetaljer(
                    type = "Ordinaer",
                    arbeidstidsordning = AaregKodeBeskrivelse(kode = "ikkeSkift", beskrivelse = "Ikke skift"),
                    yrke = AaregKodeBeskrivelse(kode = "3310101", beskrivelse = "ALLMENNLÆRER"),
                    antallTimerPrUke = 37.5,
                    avtaltStillingsprosent = stillingsprosent,
                    rapporteringsmaaneder = AaregFraTil(fra = YearMonth.parse("2002-08"), til = null)
                )
            ),
            rapporteringsordning = AaregKodeBeskrivelse(
                kode = "A_ORDNINGEN",
                beskrivelse = "Rapportert via a - ordningen(2015 - d.d.)"
            ),
            navArbeidsforholdId = 3082102,
            navVersjon = 0,
            navUuid = "92e108c0-c0e3-422d-857d-1c8a5d5f6471",
            opprettet = LocalDateTime.parse("2022-08-24T12:05:24.820"),
            sistBekreftet = LocalDateTime.parse("2022-08-24T12:05:24"),
            sistEndret = LocalDateTime.parse("2022-08-24T12:05:45"),
            bruksperiode = AaregBruksperiode(fom = LocalDateTime.parse("2022-08-24T12:05:25.041"), null)
        ).toJsonNode()
    )
)