package no.nav.etterlatte.vilkaarsvurdering.barnepensjon

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.safeLet
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Foedselsdato
import no.nav.etterlatte.vilkaarsvurdering.Hovedvilkaar
import no.nav.etterlatte.vilkaarsvurdering.Paragraf
import no.nav.etterlatte.vilkaarsvurdering.Unntaksvilkaar
import no.nav.etterlatte.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.vilkaarsvurdering.VilkaarOpplysningsType
import no.nav.etterlatte.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.vilkaarsvurdering.Vilkaarsgrunnlag

fun barnepensjonVilkaar(grunnlag: Grunnlag) = listOf(
    formaal(),
    forutgaaendeMedlemskap(),
    fortsattMedlemskap(),
    fortsattMedlemskapUnntaksbestemmelsene(),
    alderBarn(grunnlag.soeker, grunnlag.hentAvdoed()),
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

fun formaal() = Vilkaar(
    Hovedvilkaar(
        type = VilkaarType.FORMAAL,
        paragraf = Paragraf(
            paragraf = "§ 18-1",
            ledd = 1,
            tittel = "Formål",
            lenke = Kapittel18.PARAGRAF_18_1.lenke,
            lovtekst = "Formålet med barnepensjon er å sikre inntekt for barn når en av foreldrene eller begge er døde."
        )
    )
)

private fun forutgaaendeMedlemskap() = Vilkaar(
    hovedvilkaar = Hovedvilkaar(
        type = VilkaarType.FORUTGAAENDE_MEDLEMSKAP,
        paragraf = Paragraf(
            paragraf = "§ 18-2",
            ledd = 1,
            tittel = "Avdødes forutgående medlemskap",
            lenke = Kapittel18.PARAGRAF_18_2.lenke,
            lovtekst = "Det er et vilkår for rett til barnepensjon at a) den avdøde faren eller moren var medlem i " +
                "trygden de siste fem årene fram til dødsfallet, eller b) at den avdøde faren eller moren mottok " +
                "pensjon eller uføretrygd fra folketrygden de siste fem årene fram til dødsfallet."
        )
    ),
    unntaksvilkaar = listOf(
        forutgaaendeMedlemskapAvdoedIkkeFylt26Aar(),
        forutgaaendeMedlemskapAvdoedMedlemEtter16AarMedUnntakAvMaksimum5Aar(),
        forutgaaendeMedlemskapAvdoedHalvMinstepensjon()
    )
)

private fun forutgaaendeMedlemskapAvdoedIkkeFylt26Aar() = Unntaksvilkaar(
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

private fun forutgaaendeMedlemskapAvdoedMedlemEtter16AarMedUnntakAvMaksimum5Aar() = Unntaksvilkaar(
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

private fun forutgaaendeMedlemskapAvdoedHalvMinstepensjon() = Unntaksvilkaar(
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
    hovedvilkaar = Hovedvilkaar(
        type = VilkaarType.FORTSATT_MEDLEMSKAP,
        paragraf = Paragraf(
            paragraf = "§ 18-3",
            ledd = 1,
            tittel = "Fortsatt medlemskap",
            lenke = Kapittel18.PARAGRAF_18_3.lenke,
            lovtekst = "Det er et vilkår for at et barn skal ha rett til pensjon, at det fortsatt er medlem i trygden."
        )
    ),
    unntaksvilkaar = listOf(
        fortsattMedlemskapMinst20AarBotid(),
        fortsattMedlemskapAvdoedMindre20AarRettTilleggspensjon(),
        fortsattMedlemskapMinstEttBarnMedlemTrygden()
    )
)

private fun fortsattMedlemskapMinstEttBarnMedlemTrygden() = Unntaksvilkaar(
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

private fun fortsattMedlemskapAvdoedMindre20AarRettTilleggspensjon() = Unntaksvilkaar(
    type = VilkaarType.FORTSATT_MEDLEMSKAP_UNNTAK_AVDOED_MINDRE_20_AAR_BOTID_RETT_TILLEGGSPENSJON,
    paragraf = Paragraf(
        paragraf = "§ 18-3",
        ledd = 2,
        bokstav = "b",
        tittel = "Minst ett av barna i et foreldreløst barnekull er medlem i trygden",
        lovtekst = "den avdøde har mindre enn 20 års botid, men har opptjent rett til tilleggspensjon"
    )
)

private fun fortsattMedlemskapMinst20AarBotid() = Unntaksvilkaar(
    type = VilkaarType.FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRE_MINST_20_AAR_SAMLET_BOTID,
    paragraf = Paragraf(
        paragraf = "§ 18-3",
        ledd = 2,
        bokstav = "a",
        tittel = "En av foreldrene har minst 20 års samlet botid",
        lovtekst = "en av foreldrene har minst 20 års samlet botid etter § 3-5 åttende ledd"
    )
)

private fun fortsattMedlemskapUnntaksbestemmelsene() = Vilkaar(
    hovedvilkaar = Hovedvilkaar(
        type = VilkaarType.FORTSATT_MEDLEMSKAP_UNNTAKSBESTEMMELSENE,
        paragraf = Paragraf(
            paragraf = "§ 18-3",
            ledd = 3,
            tittel = "Barnepensjon etter unntaksbestemmelsene i § 18-2 tredje, femte og sjette ledd beholdes bare så " +
                "lenge barnet er medlem i trygden",
            lovtekst = "Barnepensjon etter unntaksbestemmelsene i § 18-2 tredje, femte og sjette ledd beholdes " +
                "bare så lenge barnet er medlem i trygden"
        )
    )
)

private fun alderBarn(soeker: Grunnlagsdata<JsonNode>?, avdoed: Grunnlagsdata<JsonNode>?): Vilkaar {
    val barnGrunnlag = safeLet(soeker?.hentFoedselsdato(), soeker?.hentFoedselsnummer()) { foedselsdato, fnr ->
        Vilkaarsgrunnlag(
            id = foedselsdato.id,
            opplysningsType = VilkaarOpplysningsType.FOEDSELSDATO,
            kilde = foedselsdato.kilde,
            opplysning = Foedselsdato(foedselsdato.verdi, fnr.verdi)
        )
    }
    val avdoedGrunnlag = safeLet(avdoed?.hentDoedsdato(), avdoed?.hentFoedselsnummer()) { doedsdato, fnr ->
        Vilkaarsgrunnlag(
            id = doedsdato.id,
            opplysningsType = VilkaarOpplysningsType.DOEDSDATO,
            kilde = doedsdato.kilde,
            opplysning = Doedsdato(doedsdato.verdi, fnr.verdi)
        )
    }

    return Vilkaar(
        hovedvilkaar = Hovedvilkaar(
            type = VilkaarType.ALDER_BARN,
            paragraf = Paragraf(
                paragraf = "§ 18-4",
                ledd = 1,
                tittel = "Stønadssituasjonen – barnets alder",
                lenke = Kapittel18.PARAGRAF_18_4.lenke,
                lovtekst = "Pensjon ytes inntil barnet fyller 18 år."
            )
        ),
        unntaksvilkaar = listOf(
            alderBarnBeggeForeldreDoedeUtdanning()
        ),
        grunnlag = listOfNotNull(barnGrunnlag, avdoedGrunnlag)
    )
}

private fun doedsfallForelder() = Vilkaar(
    hovedvilkaar = Hovedvilkaar(
        type = VilkaarType.DOEDSFALL_FORELDER,
        paragraf = Paragraf(
            paragraf = "§ 18-4",
            ledd = 2,
            tittel = "Dødsfall forelder",
            lenke = Kapittel18.PARAGRAF_18_4.lenke,
            lovtekst = "Barnepensjon ytes dersom en av foreldrene eller begge er døde. " +
                "Bestemmelsen i § 17-2 andre ledd om forsvunnet ektefelle gjelder tilsvarende."
        )
    )
)

private fun alderBarnBeggeForeldreDoedeUtdanning() = Unntaksvilkaar(
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
    hovedvilkaar = Hovedvilkaar(
        type = VilkaarType.YRKESSKADE_AVDOED,
        paragraf = Paragraf(
            paragraf = "§ 18-11",
            ledd = 1,
            lenke = Kapittel18.PARAGRAF_18_11.lenke,
            tittel = "Dødsfall som skyldes yrkesskade",
            lovtekst = "Ved dødsfall som skyldes en skade eller sykdom som går inn under kapittel 13, ytes det " +
                "barnepensjon etter følgende særbestemmelser:\n" +
                "\n" +
                "a.\tVilkåret i § 18-2 om forutgående medlemskap gjelder ikke.\n" +
                "b.\tVilkåret i § 18-3 om fortsatt medlemskap gjelder ikke.\n" +
                "c.\tBestemmelsene i § 18-5 om reduksjon på grunn av manglende trygdetid gjelder ikke.\n" +
                "d.\tDersom barnet har utdanning som hovedbeskjeftigelse, ytes det pensjon inntil barnet fyller " +
                "21 år.\n" +
                "e.\tTil praktikanter og lærlinger ytes det barnepensjon inntil barnet fyller 21 år, dersom " +
                "arbeidsinntekten etter fradrag for skatt er mindre enn grunnbeløpet pluss særtillegg for enslige"
        )
    )
)