package no.nav.etterlatte.libs.common.vilkaarsvurdering

enum class VilkaarType(
    val rekkefoelge: Int,
    val tittel: String,
) {
    // Barnepensjon gammelt regelverk
    BP_FORMAAL(10, "Lever barnet?"),
    BP_DOEDSFALL_FORELDER(100, "Dødsfall forelder"),
    BP_YRKESSKADE_AVDOED(200, "Yrkesskade"),
    BP_ALDER_BARN(300, "Barnets alder"),
    BP_ALDER_BARN_UNNTAK_UTDANNING(301, "Ja. Barnet er foreldreløs og har utdanning som hovedbeskjeftigelse"),
    BP_ALDER_BARN_UNNTAK_LAERLING_PRAKTIKANT(
        302,
        """
        Ja. Barnet er foreldreløs og er lærling/praktikant med en inntekt etter skatt på mindre enn to ganger grunnbeløpet
        """.trimIndent(),
    ),
    BP_FORTSATT_MEDLEMSKAP(400, "Barnets medlemskap"),
    BP_FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRE_MINST_20_AAR_SAMLET_BOTID(401, "Ja. En av foreldrene har minst 20 års samlet botid"),
    BP_FORTSATT_MEDLEMSKAP_UNNTAK_AVDOED_MINDRE_20_AAR_BOTID_RETT_TILLEGGSPENSJON(
        402,
        "Ja. Den avdøde har mindre enn 20 års botid, men har opptjent rett til tilleggspensjon",
    ),
    BP_FORTSATT_MEDLEMSKAP_UNNTAK_YRKESSKADE(403, "Ja. Dødsfallet skyldes en godkjent yrkes-skade/sykdom"),
    BP_FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRELOEST_BARN_I_KULL_MEDLEM_TRYGDEN(
        404,
        "Ja. Minst ett av barna i et foreldreløst barnekull er medlem i trygden",
    ),
    BP_VURDERING_AV_EKSPORT(450, "Vurdering av eksport"),
    BP_FORUTGAAENDE_MEDLEMSKAP(500, "Avdødes forutgående medlemskap - Folketrygden"),
    BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_IKKE_FYLT_26_AAR(
        501,
        """
        Ja. Avdøde var medlem ved dødsfallet og hadde ikke fylt 26 år (oppfylles tidligst fra tidspunktet avdøde ville ha vært medlem i ett år hvis dødsfallet ikke skjedde)
        """.trimIndent(),
    ),
    BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_MEDLEM_ETTER_16_AAR(
        502,
        """
        Ja. Avdøde var medlem ved dødsfallet og hadde vært medlem etter fylte 16 år med unntak av 5 år (oppfylles tidligst fra tidspunktet avdøde ville ha vært medlem i ett år hvis dødsfallet ikke skjedde)
        """.trimIndent(),
    ),
    BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_HALV_MINSTEPENSJON(
        503,
        """
        Ja. Avdøde var medlem ved dødsfallet og kunne fått en ytelse på minst 1 G (har minst 20 års medlemskap, eller opptjent rett til tilleggspensjon høyere enn særtillegget)
        """.trimIndent(),
    ),
    BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_AVTALEFESTET_PENSJON(
        504,
        """
        Ja. Avdøde hadde tidsrom med avtalefestet pensjon med statstilskott, som skal likestilles med tidsrom med pensjon fra folketrygden
        """.trimIndent(),
    ),
    BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_YRKESSKADE(505, "Ja. Dødsfallet skyldes en godkjent yrkes-skade/sykdom"),
    BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_LOVFESTET_PENSJONSORDNING(
        506,
        """
        Ja. Avdøde hadde tidsrom med pensjon fra en lovfestet pensjonsordning som er tilpasset folketrygden ved at det ikke gis ordinær barnepensjon
        """.trimIndent(),
    ),
    BP_FORUTGAAENDE_MEDLEMSKAP_EOES(600, "Avdødes forutgående medlemskap - EØS/avtaleland"),

    // Barnepensjon nytt regelverk
    BP_FORMAAL_2024(10, "Lever barnet?"),
    BP_DOEDSFALL_FORELDER_2024(100, "Dødsfall forelder"),
    BP_YRKESSKADE_AVDOED_2024(200, "Yrkesskade"),
    BP_ALDER_BARN_2024(300, "Barnets alder"),
    BP_ALDER_BARN_UNNTAK_YS_GAMMELT_REGELVERK(
        301,
        "Ja. Bruker er innvilget barnepensjon før 01.01.2024 med yrkesskade-fordel og skal ha til fylte 21 år",
    ),
    BP_FORTSATT_MEDLEMSKAP_2024(400, "Barnets medlemskap"),
    BP_FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRE_MINST_20_AAR_SAMLET_BOTID_2024(401, "Ja. En av foreldrene har minst 20 års samlet botid"),
    BP_FORTSATT_MEDLEMSKAP_UNNTAK_AVDOED_MINDRE_20_AAR_BOTID_TRE_POENGAAR_2024(
        402,
        """
        Ja. Den avdøde har mindre enn 20 års botid, men har minimum tre poengår. Trygdetid beregnes bare etter faktiske poengår
        """.trimIndent(),
    ),
    BP_FORTSATT_MEDLEMSKAP_UNNTAK_YRKESSKADE_2024(403, "Ja. Dødsfallet skyldes en godkjent yrkes-skade/sykdom"),
    BP_FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRELOEST_BARN_I_KULL_MEDLEM_TRYGDEN_2024(
        404,
        "Ja. Minst ett av barna i et foreldreløst barnekull er medlem i trygden",
    ),
    BP_VURDERING_AV_EKSPORT_2024(450, "Vurdering av eksport"),
    BP_FORUTGAAENDE_MEDLEMSKAP_2024(500, "Avdødes forutgående medlemskap - Folketrygden"),
    BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_IKKE_FYLT_26_AAR_2024(
        501,
        """
        Ja. Avdøde var medlem ved dødsfallet og hadde ikke fylt 26 år (oppfylles tidligst fra tidspunktet avdøde ville ha vært medlem i ett år hvis dødsfallet ikke skjedde)
        """.trimIndent(),
    ),
    BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_MEDLEM_ETTER_16_AAR_2024(
        502,
        """
        Ja. Avdøde var medlem ved dødsfallet og hadde vært medlem etter fylte 16 år med unntak av 5 år (oppfylles tidligst fra tidspunktet avdøde ville ha vært medlem i ett år hvis dødsfallet ikke skjedde)
        """.trimIndent(),
    ),
    BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_HALV_MINSTEPENSJON_2024(
        503,
        """
        Ja. Avdøde var medlem ved dødsfallet og kunne fått en ytelse på minst 1 G (har minst 20 års medlemskap, eller opptjent rett til tilleggspensjon høyere enn særtillegget)
        """.trimIndent(),
    ),
    BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_AVTALEFESTET_PENSJON_2024(
        504,
        """
        Ja. Avdøde hadde tidsrom med avtalefestet pensjon med statstilskott, som skal likestilles med tidsrom med pensjon fra folketrygden
        """.trimIndent(),
    ),
    BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_YRKESSKADE_2024(505, "Ja. Dødsfallet skyldes en godkjent yrkes-skade/sykdom"),
    BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_LOVFESTET_PENSJONSORDNING_2024(
        506,
        """
        Ja. Avdøde hadde tidsrom med pensjon fra en lovfestet pensjonsordning som er tilpasset folketrygden ved at det ikke gis ordinær barnepensjon
        """.trimIndent(),
    ),
    BP_FORUTGAAENDE_MEDLEMSKAP_EOES_2024(600, "Avdødes forutgående medlemskap - EØS/avtaleland"),

    // Omstillingsstønad
    OMS_ETTERLATTE_LEVER(90, "Lever den etterlatte?"),
    OMS_DOEDSFALL(100, "Dødsfall ektefelle/partner/samboer"),
    OMS_OEVRIGE_VILKAAR(105, "Øvrige vilkår for ytelser"),
    OMS_OVERLAPPENDE_YTELSER(110, "Bortfall rettigheter etter § 17-11 første ledd?"),
    OMS_SIVILSTAND(120, "Rett til omstillingsstønad igjen etter § 17-11 andre ledd?"),
    OMS_YRKESSKADE(200, "Dødsfall som skyldes yrkesskade eller yrkessykdom"),
    OMS_AVDOEDES_MEDLEMSKAP(300, "Avdødes forutgående medlemskap - Folketrygden"),
    OMS_AVDOEDES_MEDLEMSKAP_UNNTAK_UNDER_26(301, "Ja, avdøde var medlem av trygden ved dødsfallet og ikke fylt 26 år"),
    OMS_AVDOEDES_MEDLEMSKAP_UNNTAK_OVER_16(
        302,
        """
        Ja, avdøde var medlem av trygden ved dødsfallet og hadde vært medlem etter fylte 16 år med unntak av 5 år
        """.trimIndent(),
    ),
    OMS_AVDOEDES_MEDLEMSKAP_UNNTAK_PENSJON(
        303,
        """
        Ja, avdøde hadde avtalefestet pensjon, eller pensjon fra en lovfestet pensjonsordning som er tilpasset folketrygden ved at det ikke gis ordinær pensjon til gjenlevende ektefelle
        """.trimIndent(),
    ),
    OMS_AVDOEDES_MEDLEMSKAP_UNNTAK_OPPTJENING(
        304,
        """
        Ja, avdøde kunne tilstås en ytelse på grunnlag av tidligere opptjening minst svarende til grunnbeløpet
        """.trimIndent(),
    ),
    OMS_AVDOEDES_MEDLEMSKAP_UNNTAK_YRKESSKADE(305, "Ja, dødsfallet skyldes en godkjent yrkes-skade/sykdom"),
    OMS_AVDOEDES_MEDLEMSKAP_EOES(350, "Avdødes forutgående medlemskap - EØS/avtaleland"),
    OMS_GJENLEVENDES_MEDLEMSKAP(400, "Gjenlevendes medlemskap"),
    OMS_GJENLEVENDES_MEDLEMSKAP_UNNTAK_BOTID(401, "Ja, den avdøde eller den gjenlevende har minst 20 års samlet botid"),
    OMS_GJENLEVENDES_MEDLEMSKAP_UNNTAK_PENSJON(
        402,
        """
        Ja, både avdøde og gjenlevende har mindre enn 20 års botid, og det gis stønad med en trygdetid lik avdødes antall poengår
        """.trimIndent(),
    ),
    OMS_GJENLEVENDES_MEDLEMSKAP_UNNTAK_YRKESSKADE(403, "Ja, dødsfallet skyldes en godkjent yrkes-skade/sykdom"),
    OMS_VURDERING_AV_EKSPORT(450, "Vurdering av eksport"),
    OMS_RETT_UTEN_TIDSBEGRENSNING(500, "Rett til stønad uten tidsbegrensning?"),
    OMS_AKTIVITETSPLIKT(600, "Langvarig manglende aktivitet"),

    // Vilkårene OMS_AKTIVITET_ETTER_6_MND og tilhørende unntak eksisterer på gamle vurderinger og kan ikke slettes.
    OMS_AKTIVITET_ETTER_6_MND(600, "Krav til aktivitet etter 6 måneder"),
    OMS_AKTIVITET_ETTER_6_MND_UNNTAK_GJENLEVENDE_OVER_55_AAR_OG_LAV_INNTEKT(
        601,
        "Ja, gjenlevende er født i 1963 eller tidligere og har hatt lav inntekt",
    ),
    OMS_AKTIVITET_ETTER_6_MND_UNNTAK_GJENLEVENDE_BARN_UNDER_1_AAR(602, "Ja, gjenlevende har barn som er under 1 år"),
    ;

    companion object {
        fun yrkesskadeVilkaarTyper() = listOf(BP_YRKESSKADE_AVDOED, BP_YRKESSKADE_AVDOED_2024, OMS_YRKESSKADE)
    }
}
