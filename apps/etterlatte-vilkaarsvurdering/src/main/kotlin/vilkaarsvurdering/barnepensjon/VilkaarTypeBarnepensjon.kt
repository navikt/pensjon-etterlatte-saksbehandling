package no.nav.etterlatte.vilkaarsvurdering.barnepensjon

import no.nav.etterlatte.vilkaarsvurdering.Paragraf
import no.nav.etterlatte.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.vilkaarsvurdering.VilkaarType

fun barnepensjonVilkaar() = listOf(
    formaal(),
    forutgaaendeMedlemskap(),
    fortsattMedlemskap(),
    alderBarn(),
    doedsfallForelder(),
    yrkesskadeAvdoed()
)

enum class Kapittel18(val paragraf: String, val lenke: String) {
    PARAGRAF_18_1("§ 18-1", "https://lovdata.no/lov/1997-02-28-19/%C2%A718-1"),
    PARAGRAF_18_2("§ 18-2", "https://lovdata.no/lov/1997-02-28-19/%C2%A718-2"),
    PARAGRAF_18_3("§ 18-3", "https://lovdata.no/lov/1997-02-28-19/%C2%A718-3"),
    PARAGRAF_18_4("§ 18-4", "https://lovdata.no/lov/1997-02-28-19/%C2%A718-4"),
    PARAGRAF_18_11("§ 18-11", "https://lovdata.no/lov/1997-02-28-19/%C2%A718-11");
}

private fun formaal() = Vilkaar(
    type = VilkaarType.FORMAAL,
    paragraf = Paragraf(
        paragraf = "§ 18-1",
        ledd = 1,
        tittel = "Formål",
        lenke = Kapittel18.PARAGRAF_18_1.lenke,
        lovtekst = "Formålet med barnepensjon er å sikre inntekt for barn når en av foreldrene eller begge er døde."
    )
)

private fun forutgaaendeMedlemskap() = Vilkaar(
    type = VilkaarType.FORUTGAAENDE_MEDLEMSKAP,
    paragraf = Paragraf(
        paragraf = "§ 18-2",
        ledd = 1,
        bokstav = "a",
        tittel = "Avdødes forutgående medlemskap",
        lenke = Kapittel18.PARAGRAF_18_2.lenke,
        lovtekst = "Det er et vilkår for rett til barnepensjon at a) den avdøde faren eller moren var medlem i " +
            "trygden de siste fem årene fram til dødsfallet, eller b) at den avdøde faren eller moren mottok " +
            "pensjon eller uføretrygd fra folketrygden de siste fem årene fram til dødsfallet."
    ),
    unntaksvilkaar = listOf(
        forutgaaendeMedlemskapAvdoedMottokPensjonEllerUfoeretrygd(),
        forutgaaendeMedlemskapAvdoedIkkeFylt26Aar(),
        forutgaaendeMedlemskapAvdoedMedlemEtter16AarMedUnntakAvMaksimum5Aar(),
        forutgaaendeMedlemskapAvdoedHalvMinstepensjon()
    )
)

private fun forutgaaendeMedlemskapAvdoedMottokPensjonEllerUfoeretrygd() = Vilkaar(
    type = VilkaarType.FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_MOTTOK_PENSJON_ELLER_UFOERETRYGD,
    paragraf = Paragraf(
        paragraf = "§ 18-2",
        ledd = 1,
        bokstav = "b",
        tittel = "Avdødemottok pensjon eller uføretrygd fra folketrygden de siste fem årene fram til dødsfallet",
        lovtekst = "b) at den avdøde faren eller moren mottok pensjon eller uføretrygd fra folketrygden de siste fem " +
            "årene fram til dødsfallet."
    )
)

private fun forutgaaendeMedlemskapAvdoedIkkeFylt26Aar() = Vilkaar(
    type = VilkaarType.FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_IKKE_FYLT_26_AAR,
    paragraf = Paragraf(
        paragraf = "§ 18-2",
        ledd = 3,
        bokstav = "a",
        tittel = "Avdøde medlem av trygden ved dødsfallet og ikke fylt 26 år",
        lovtekst = "Vilkåret i første ledd gjelder ikke dersom den avdøde ved dødsfallet var medlem i trygden og da " +
            "a) ikke hadde fylt 26 år, eller b) hadde vært medlem etter fylte 16 år med unntak av " +
            "maksimum 5 år."
    )
)

private fun forutgaaendeMedlemskapAvdoedMedlemEtter16AarMedUnntakAvMaksimum5Aar() = Vilkaar(
    type = VilkaarType.FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_MEDLEM_ETTER_16_AAR,
    paragraf = Paragraf(
        paragraf = "§ 18-2",
        ledd = 3,
        bokstav = "b",
        tittel = "Avdøde medlem av trygden ved dødsfallet og hadde vært medlem etter fylte 16 år med unntak av 5 år",
        lovtekst = "Vilkåret i første ledd gjelder ikke dersom den avdøde ved dødsfallet var medlem i trygden og da " +
            "a) ikke hadde fylt 26 år, eller b) hadde vært medlem etter fylte 16 år med unntak av " +
            "maksimum 5 år."
    )
)

private fun forutgaaendeMedlemskapAvdoedHalvMinstepensjon() = Vilkaar(
    type = VilkaarType.FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_HALV_MINSTEPENSJON,
    paragraf = Paragraf(
        paragraf = "§ 18-2",
        ledd = 6,
        tittel = "Avdøde kunne tilstås en ytelse på grunnlag av tidligere opptjening",
        lovtekst = "Vilkåret i første ledd gjelder ikke når den avdøde faren eller moren var medlem i folketrygden " +
            "ved dødsfallet og kunne tilstås en ytelse på grunnlag av tidligere opptjening minst svarende til " +
            "halvparten av full minstepensjon. Med «ytelse på grunnlag av tidligere opptjening» menes en ytelse " +
            "beregnet etter reglene for alderspensjon etter kapittel 3 på grunnlag av poengår og perioder som " +
            "medlem av folketrygden før dødsfallet."
    )
)

private fun fortsattMedlemskap() = Vilkaar(
    type = VilkaarType.FORTSATT_MEDLEMSKAP,
    paragraf = Paragraf(
        paragraf = "§ 18-3",
        ledd = 1,
        tittel = "Fortsatt medlemskap",
        lenke = Kapittel18.PARAGRAF_18_3.lenke,
        lovtekst = "Det er et vilkår for at et barn skal ha rett til pensjon, at det fortsatt er medlem i trygden."
    ),
    unntaksvilkaar = listOf(
        fortsattMedlemskapMinst20AarBotid(),
        fortsattMedlemskapAvdoedMindre20AarRettTilleggspensjon(),
        fortsattMedlemskapMinstEttBarnMedlemTrygden()
    )
)

private fun fortsattMedlemskapMinstEttBarnMedlemTrygden() = Vilkaar(
    type = VilkaarType.FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRELOEST_BARN_I_KULL_MEDLEM_TRYGDEN,
    paragraf = Paragraf(
        paragraf = "§ 18-3",
        ledd = 2,
        bokstav = "c",
        tittel = "Minst ett av barna i et foreldreløst barnekull er medlem i trygden",
        lovtekst = "minst ett av barna i et foreldreløst barnekull er medlem i trygden. Dette gjelder selv om det " +
            "barnet som er medlem i trygden, har passert aldersgrensen for rett til barnepensjon."
    )
)

private fun fortsattMedlemskapAvdoedMindre20AarRettTilleggspensjon() = Vilkaar(
    type = VilkaarType.FORTSATT_MEDLEMSKAP_UNNTAK_AVDOED_MINDRE_20_AAR_BOTID_RETT_TILLEGGSPENSJON,
    paragraf = Paragraf(
        paragraf = "§ 18-3",
        ledd = 2,
        bokstav = "b",
        tittel = "Minst ett av barna i et foreldreløst barnekull er medlem i trygden",
        lovtekst = "den avdøde har mindre enn 20 års botid, men har opptjent rett til tilleggspensjon"
    )
)

private fun fortsattMedlemskapMinst20AarBotid() = Vilkaar(
    type = VilkaarType.FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRE_MINST_20_AAR_SAMLET_BOTID,
    paragraf = Paragraf(
        paragraf = "§ 18-3",
        ledd = 2,
        bokstav = "a",
        tittel = "En av foreldrene har minst 20 års samlet botid",
        lovtekst = "en av foreldrene har minst 20 års samlet botid etter § 3-5 åttende ledd"
    )
)

private fun alderBarn() = Vilkaar(
    type = VilkaarType.ALDER_BARN,
    paragraf = Paragraf(
        paragraf = "§ 18-4",
        ledd = 1,
        tittel = "Stønadssituasjonen – barnets alder",
        lenke = Kapittel18.PARAGRAF_18_4.lenke,
        lovtekst = "Pensjon ytes inntil barnet fyller 18 år."
    ),
    unntaksvilkaar = listOf(
        alderBarnBeggeForeldreDoedeUtdanning()
    )
)

private fun doedsfallForelder() = Vilkaar(
    type = VilkaarType.DOEDSFALL_FORELDER,
    paragraf = Paragraf(
        paragraf = "§ 18-4",
        ledd = 2,
        tittel = "Dødsfall forelder",
        lenke = Kapittel18.PARAGRAF_18_4.lenke,
        lovtekst = "Barnepensjon ytes dersom en av foreldrene eller begge er døde. Bestemmelsen i § 17-2 andre ledd " +
            "om forsvunnet ektefelle gjelder tilsvarende."
    )
)

private fun alderBarnBeggeForeldreDoedeUtdanning() = Vilkaar(
    type = VilkaarType.ALDER_BARN_UNNTAK_UTDANNING,
    paragraf = Paragraf(
        paragraf = "§ 18-4",
        ledd = 3,
        tittel = "Begge foreldrene er døde og barnet har utdanning som hovedbeskjeftigelse",
        lovtekst = "Dersom begge foreldrene er døde og barnet har utdanning som hovedbeskjeftigelse, ytes det " +
            "pensjon inntil barnet fyller 20 år. Praktikanter og lærlinger har rett til barnepensjon inntil " +
            "fylte 20 år dersom begge foreldrene er døde og arbeidsinntekten etter fradrag for skatt er " +
            "mindre enn grunnbeløpet pluss særtillegg for enslige. Bestemmelsene i dette leddet gjelder " +
            "også når moren er død og farskapet ikke er fastslått."
    )
)

private fun yrkesskadeAvdoed() = Vilkaar(
    type = VilkaarType.YRKESSKADE_AVDOED,
    paragraf = Paragraf(
        paragraf = "§ 18-11",
        ledd = 1,
        lenke = Kapittel18.PARAGRAF_18_11.lenke,
        tittel = "Dødsfall som skyldes yrkesskade",
        lovtekst = ""
    )
)