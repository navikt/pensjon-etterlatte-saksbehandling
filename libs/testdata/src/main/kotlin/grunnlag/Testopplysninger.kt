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
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SaksbehandlerMedlemskapsperiode
import no.nav.etterlatte.libs.common.inntekt.ArbeidsInntektInformasjon
import no.nav.etterlatte.libs.common.inntekt.Inntekt
import no.nav.etterlatte.libs.common.inntekt.InntektType
import no.nav.etterlatte.libs.common.inntekt.Inntektsmottaker
import no.nav.etterlatte.libs.common.inntekt.Opplysningspliktig
import no.nav.etterlatte.libs.common.inntekt.Virksomhet
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.UtenlandsoppholdOpplysninger
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.OppholdUtlandType
import no.nav.etterlatte.libs.common.toJsonNode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

val tidspunkt = Instant.now()

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

val inntekter: List<Grunnlagsopplysning<ArbeidsInntektInformasjon>> = listOf(
    Grunnlagsopplysning(
        id = UUID.fromString("57888a70-b430-4e30-b1dd-7ec504da7156"),
        kilde = Grunnlagsopplysning.Aordningen(tidspunkt = tidspunkt),
        opplysningType = Opplysningstype.INNTEKT,
        meta = objectMapper.createObjectNode(),
        opplysning = ArbeidsInntektInformasjon(
            inntektListe = listOf(
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2017-09",
                    opplysningspliktig =
                    Opplysningspliktig(identifikator = "928497704", aktoerType = "ORGANISASJON"),
                    virksomhet = Virksomhet(identifikator = "967170232", aktoerType = "ORGANISASJON"),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                ),
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2017-09",
                    opplysningspliktig = Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(identifikator = "967170232", aktoerType = "ORGANISASJON"),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                ),
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2017-09",
                    opplysningspliktig = Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(identifikator = "967170232", aktoerType = "ORGANISASJON"),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                )
            )
        ),
        attestering = null,
        fnr = Foedselsnummer.of("01448203510"),
        periode = Periode(
            fom = YearMonth.parse("2017-09"),
            tom = YearMonth.parse("2017-09")
        )
    ),
    Grunnlagsopplysning(
        id = UUID.fromString("34be674d-f536-40e3-b8b6-09118eb4fe53"),
        kilde = Grunnlagsopplysning.Aordningen(tidspunkt = tidspunkt),
        opplysningType = Opplysningstype.INNTEKT,
        meta = objectMapper.createObjectNode(),
        opplysning = ArbeidsInntektInformasjon(
            inntektListe = listOf(
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2017-10",
                    opplysningspliktig =
                    Opplysningspliktig(identifikator = "928497704", aktoerType = "ORGANISASJON"),
                    virksomhet = Virksomhet(identifikator = "967170232", aktoerType = "ORGANISASJON"),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                ),
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2017-10",
                    opplysningspliktig = Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(identifikator = "967170232", aktoerType = "ORGANISASJON"),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                ),
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2017-10",
                    opplysningspliktig = Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(identifikator = "967170232", aktoerType = "ORGANISASJON"),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                )
            )
        ),
        attestering = null,
        fnr = Foedselsnummer.of("01448203510"),
        periode = Periode(
            fom = YearMonth.parse("2017-10"),
            tom = YearMonth.parse("2017-10")
        )
    ),
    Grunnlagsopplysning(
        id = UUID.fromString("a31c0010-fca0-4604-97fb-c1e41af90a72"),
        kilde = Grunnlagsopplysning.Aordningen(tidspunkt = tidspunkt),
        opplysningType = Opplysningstype.INNTEKT,
        meta = objectMapper.createObjectNode(),
        opplysning = ArbeidsInntektInformasjon(
            inntektListe = listOf(
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2017-11",
                    opplysningspliktig =
                    Opplysningspliktig(identifikator = "928497704", aktoerType = "ORGANISASJON"),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                ),
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2017-11",
                    opplysningspliktig = Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                ),
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2017-11",
                    opplysningspliktig = Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                )
            )
        ),
        attestering = null,
        fnr = Foedselsnummer.of("01448203510"),
        periode = Periode(
            fom = YearMonth.parse("2017-11"),
            tom = YearMonth.parse("2017-11")
        )
    ),
    Grunnlagsopplysning(
        id = UUID.fromString("237c473d-fccd-4908-a43f-c8c879a54020"),
        kilde = Grunnlagsopplysning.Aordningen(tidspunkt = tidspunkt),
        opplysningType = Opplysningstype.INNTEKT,
        meta = objectMapper.createObjectNode(),
        opplysning = ArbeidsInntektInformasjon(
            inntektListe = listOf(
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2017-12",
                    opplysningspliktig =
                    Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                ),
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2017-12",
                    opplysningspliktig = Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                ),
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2017-12",
                    opplysningspliktig = Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                )
            )
        ),
        attestering = null,
        fnr = Foedselsnummer.of("01448203510"),
        periode = Periode(
            fom = YearMonth.parse("2017-12"),
            tom = YearMonth.parse("2017-12")
        )
    ),
    Grunnlagsopplysning(
        id = UUID.fromString("44f963f8-200d-4d80-8003-210497db0196"),
        kilde = Grunnlagsopplysning.Aordningen(tidspunkt = tidspunkt),
        opplysningType = Opplysningstype.INNTEKT,
        meta = objectMapper.createObjectNode(),
        opplysning = ArbeidsInntektInformasjon(
            inntektListe = listOf(
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2017-08",
                    opplysningspliktig =
                    Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                ),
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2017-08",
                    opplysningspliktig = Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                ),
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2017-08",
                    opplysningspliktig = Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                )
            )
        ),
        attestering = null,
        fnr = Foedselsnummer.of("01448203510"),
        periode = Periode(
            fom = YearMonth.parse("2017-08"),
            tom = YearMonth.parse("2017-08")
        )
    ),
    Grunnlagsopplysning(
        id = UUID.fromString("760ea7d3-5fab-4de8-8649-2757333a6c8c"),
        kilde = Grunnlagsopplysning.Aordningen(tidspunkt = tidspunkt),
        opplysningType = Opplysningstype.INNTEKT,
        meta = objectMapper.createObjectNode(),
        opplysning = ArbeidsInntektInformasjon(
            inntektListe = listOf(
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2018-06",
                    opplysningspliktig =
                    Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                ),
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2018-06",
                    opplysningspliktig = Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                ),
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2018-06",
                    opplysningspliktig = Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                )
            )
        ),
        attestering = null,
        fnr = Foedselsnummer.of("01448203510"),
        periode = Periode(
            fom = YearMonth.parse("2018-06"),
            tom = YearMonth.parse("2018-06")
        )
    ),
    Grunnlagsopplysning(
        id = UUID.fromString("cc64c5ee-7ee6-4eb6-bb14-e7332601dc72"),
        kilde = Grunnlagsopplysning.Aordningen(tidspunkt = tidspunkt),
        opplysningType = Opplysningstype.INNTEKT,
        meta = objectMapper.createObjectNode(),
        opplysning = ArbeidsInntektInformasjon(
            inntektListe = listOf(
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2018-04",
                    opplysningspliktig =
                    Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                ),
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2018-04",
                    opplysningspliktig = Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                ),
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2018-04",
                    opplysningspliktig = Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                )
            )
        ),
        attestering = null,
        fnr = Foedselsnummer.of("01448203510"),
        periode = Periode(
            fom = YearMonth.parse("2018-04"),
            tom = YearMonth.parse("2018-04")
        )
    ),
    Grunnlagsopplysning(
        id = UUID.fromString("8911850f-ebd6-4592-9ccf-66a22b510de2"),
        kilde = Grunnlagsopplysning.Aordningen(tidspunkt = tidspunkt),
        opplysningType = Opplysningstype.INNTEKT,
        meta = objectMapper.createObjectNode(),
        opplysning = ArbeidsInntektInformasjon(
            inntektListe = listOf(
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2018-03",
                    opplysningspliktig =
                    Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                ),
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2018-03",
                    opplysningspliktig = Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                ),
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2018-03",
                    opplysningspliktig = Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                )
            )
        ),
        attestering = null,
        fnr = Foedselsnummer.of("01448203510"),
        periode = Periode(
            fom = YearMonth.parse("2018-03"),
            tom = YearMonth.parse("2018-03")
        )
    ),
    Grunnlagsopplysning(
        id = UUID.fromString("93349f2c-aee5-464a-8c10-ca6dee58eab2"),
        kilde = Grunnlagsopplysning.Aordningen(tidspunkt = tidspunkt),
        opplysningType = Opplysningstype.INNTEKT,
        meta = objectMapper.createObjectNode(),
        opplysning = ArbeidsInntektInformasjon(
            inntektListe = listOf(
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt =
                    "2022-08",
                    utbetaltIMaaned =
                    "2018-09",
                    opplysningspliktig =
                    Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                ),
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned =
                    "2018-09",
                    opplysningspliktig = Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                ),
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2018-09",
                    opplysningspliktig = Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                )
            )
        ),
        attestering = null,
        fnr = Foedselsnummer.of("01448203510"),
        periode = Periode(
            fom = YearMonth.parse("2018-09"),
            tom = YearMonth.parse("2018-09")
        )
    ),
    Grunnlagsopplysning(
        id = UUID.fromString("299e4a81-19b0-4f81-b62f-d389bf58ebb7"),
        kilde = Grunnlagsopplysning.Aordningen(tidspunkt = tidspunkt),
        opplysningType = Opplysningstype.INNTEKT,
        meta = objectMapper.createObjectNode(),
        opplysning = ArbeidsInntektInformasjon(
            inntektListe = listOf(
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2018-10",
                    opplysningspliktig =
                    Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                ),
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2018-10",
                    opplysningspliktig = Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                ),
                Inntekt(
                    inntektType = InntektType.PENSJON_ELLER_TRYGD,
                    beloep = 27000,
                    fordel = "kontantytelse",
                    inntektskilde = "A-ordningen",
                    inntektsperiodetype = "Maaned",
                    inntektsstatus = "LoependeInnrapportert",
                    leveringstidspunkt = "2022-08",
                    utbetaltIMaaned = "2018-10",
                    opplysningspliktig = Opplysningspliktig(
                        identifikator = "928497704",
                        aktoerType = "ORGANISASJON"
                    ),
                    virksomhet = Virksomhet(
                        identifikator = "967170232",
                        aktoerType = "ORGANISASJON"
                    ),
                    inntektsmottaker = Inntektsmottaker(
                        identifikator = "12526320512",
                        aktoerType = "NATURLIG_IDENT"
                    ),
                    inngaarIGrunnlagForTrekk = true,
                    utloeserArbeidsgiveravgift = false,
                    informasjonsstatus = "InngaarAlltid",
                    beskrivelse = "alderspensjon",
                    skatteOgAvgiftsregel = null
                )
            )
        ),
        attestering = null,
        fnr = Foedselsnummer.of("01448203510"),
        periode = Periode(
            fom = YearMonth.parse("2018-10"),
            tom = YearMonth.parse("2018-10")
        )
    )
)

val inntektsopplysning = inntekter.map {
    PeriodisertOpplysning(
        id = it.id,
        kilde = it.kilde,
        verdi = it.opplysning.toJsonNode(),
        fom = it.periode!!.fom,
        tom = it.periode?.tom
    )
}