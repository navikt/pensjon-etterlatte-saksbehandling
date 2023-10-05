package no.nav.etterlatte.libs.common.vilkaarsvurdering

enum class VilkaarType(val rekkefoelge: Int) {
    // Barnepensjon
    BP_FORMAAL(10),
    BP_DOEDSFALL_FORELDER(100),
    BP_YRKESSKADE_AVDOED(200),
    BP_ALDER_BARN(300),
    BP_ALDER_BARN_UNNTAK_UTDANNING(301),
    BP_ALDER_BARN_UNNTAK_LAERLING_PRAKTIKANT(302),
    BP_FORTSATT_MEDLEMSKAP(400),
    BP_FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRE_MINST_20_AAR_SAMLET_BOTID(401),
    BP_FORTSATT_MEDLEMSKAP_UNNTAK_AVDOED_MINDRE_20_AAR_BOTID_RETT_TILLEGGSPENSJON(402),
    BP_FORTSATT_MEDLEMSKAP_UNNTAK_YRKESSKADE(403),
    BP_FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRELOEST_BARN_I_KULL_MEDLEM_TRYGDEN(404),
    BP_FORUTGAAENDE_MEDLEMSKAP(500),
    BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_IKKE_FYLT_26_AAR(501),
    BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_MEDLEM_ETTER_16_AAR(502),
    BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_HALV_MINSTEPENSJON(503),
    BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_AVTALEFESTET_PENSJON(504),
    BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_YRKESSKADE(505),
    BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_LOVFESTET_PENSJONSORDNING(506),
    BP_FORUTGAAENDE_MEDLEMSKAP_EOES(600),

    // Omstillingsstønad
    OMS_ETTERLATTE_LEVER(90),
    OMS_DOEDSFALL(100),
    OMS_OVERLAPPENDE_YTELSER(110),
    OMS_SIVILSTAND(120),
    OMS_YRKESSKADE(200),
    OMS_AVDOEDES_MEDLEMSKAP(300),
    OMS_AVDOEDES_MEDLEMSKAP_UNNTAK_UNDER_26(301),
    OMS_AVDOEDES_MEDLEMSKAP_UNNTAK_OVER_16(302),
    OMS_AVDOEDES_MEDLEMSKAP_UNNTAK_PENSJON(303),
    OMS_AVDOEDES_MEDLEMSKAP_UNNTAK_OPPTJENING(304),
    OMS_AVDOEDES_MEDLEMSKAP_UNNTAK_YRKESSKADE(305),
    OMS_AVDOEDES_MEDLEMSKAP_EOES(350),
    OMS_GJENLEVENDES_MEDLEMSKAP(400),
    OMS_GJENLEVENDES_MEDLEMSKAP_UNNTAK_BOTID(401),
    OMS_GJENLEVENDES_MEDLEMSKAP_UNNTAK_PENSJON(402),
    OMS_GJENLEVENDES_MEDLEMSKAP_UNNTAK_YRKESSKADE(403),
    OMS_OEVRIGE_VILKAAR(500),
    OMS_AKTIVITET_ETTER_6_MND(600),
    OMS_AKTIVITET_ETTER_6_MND_UNNTAK_GJENLEVENDE_OVER_55_AAR_OG_LAV_INNTEKT(601),
    OMS_AKTIVITET_ETTER_6_MND_UNNTAK_GJENLEVENDE_BARN_UNDER_1_AAR(602),
    ;

    companion object {
        fun yrkesskadeVilkaarTyper() = listOf(BP_YRKESSKADE_AVDOED, OMS_YRKESSKADE)
    }
}
