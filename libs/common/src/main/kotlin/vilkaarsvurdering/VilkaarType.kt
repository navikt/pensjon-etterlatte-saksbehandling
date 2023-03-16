package no.nav.etterlatte.libs.common.vilkaarsvurdering

enum class VilkaarType(val rekkefoelge: Int) {
    // Barnepensjon
    BP_DOEDSFALL_FORELDER(100),
    BP_ALDER_BARN(200),
    BP_ALDER_BARN_UNNTAK_UTDANNING(201),
    BP_ALDER_BARN_UNNTAK_LAERLING_PRAKTIKANT(202),
    BP_FORTSATT_MEDLEMSKAP(300),
    BP_FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRE_MINST_20_AAR_SAMLET_BOTID(301),
    BP_FORTSATT_MEDLEMSKAP_UNNTAK_AVDOED_MINDRE_20_AAR_BOTID_RETT_TILLEGGSPENSJON(302),
    BP_FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRELOEST_BARN_I_KULL_MEDLEM_TRYGDEN(303),
    BP_FORUTGAAENDE_MEDLEMSKAP(400),
    BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_IKKE_FYLT_26_AAR(401),
    BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_MEDLEM_ETTER_16_AAR(402),
    BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_HALV_MINSTEPENSJON(403),
    BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_AVTALEFESTET_PENSJON(404),
    BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_LOVFESTET_PENSJONSORDNING(405),
    BP_YRKESSKADE_AVDOED(500),

    // Omstillingsstønad
    OMS_DOEDSFALL(100),
    OMS_YRKESSKADE(200),
    OMS_AVDOEDES_MEDLEMSKAP(300),
    OMS_AVDOEDES_MEDLEMSKAP_UNNTAK_UNDER_26(301),
    OMS_AVDOEDES_MEDLEMSKAP_UNNTAK_OVER_16(302),
    OMS_AVDOEDES_MEDLEMSKAP_UNNTAK_PENSJON(303),
    OMS_AVDOEDES_MEDLEMSKAP_UNNTAK_OPPTJENING(304),
    OMS_GJENLEVENDES_MEDLEMSKAP(400),
    OMS_GJENLEVENDES_MEDLEMSKAP_UNNTAK_BOTID(401),
    OMS_GJENLEVENDES_MEDLEMSKAP_UNNTAK_PENSJON(402),
    OMS_OEVRIGE_VILKAAR(500)
}