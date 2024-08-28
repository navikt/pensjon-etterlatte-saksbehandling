package no.nav.etterlatte.vilkaarsvurdering.vilkaar

import no.nav.etterlatte.libs.common.vilkaarsvurdering.Delvilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Lovreferanse
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType

object BarnepensjonVilkaar1967 {
    fun inngangsvilkaar() =
        listOf(
            formaal(),
            doedsfallForelder(),
            yrkesskadeAvdoed(),
            alderBarn(),
            barnetsMedlemskap(),
            vurderingAvEksport(),
            avdoedesForutgaaendeMedlemskap(),
            avdoedesForutgaaendeMedlemskapEoes(),
        )

    private fun formaal() =
        Vilkaar(
            hovedvilkaar =
                Delvilkaar(
                    type = VilkaarType.BP_FORMAAL,
                    beskrivelse =
                        """
                        Formålet med barnepensjon er å sikre inntekt for barn når en av foreldrene eller begge er døde. Dette betyr at barnet må være i live for å ha rett på barnepensjon.
                        """.trimIndent(),
                    spoersmaal = "Lever barnet det søkes barnepensjon for på virkningstidspunktet?",
                    lovreferanse =
                        Lovreferanse(
                            paragraf = "§ 18-1",
                            ledd = 1,
                            lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-1",
                        ),
                ),
        )

    private fun doedsfallForelder() =
        Vilkaar(
            hovedvilkaar =
                Delvilkaar(
                    type = VilkaarType.BP_DOEDSFALL_FORELDER,
                    beskrivelse =
                        """
                        For å ha rett på ytelsen må en eller begge foreldre være registrer død i folkeregisteret eller hos utenlandske myndigheter.
                        """.trimIndent(),
                    spoersmaal = "Er en eller begge foreldrene registrert som død?",
                    lovreferanse =
                        Lovreferanse(
                            paragraf = "§ 18-4",
                            ledd = 2,
                            lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-4",
                        ),
                ),
        )

    private fun alderBarn(): Vilkaar =
        Vilkaar(
            hovedvilkaar =
                Delvilkaar(
                    type = VilkaarType.BP_ALDER_BARN,
                    beskrivelse = "For å ha rett på ytelsen må barnet være under 18 år på virkningstidspunktet.",
                    spoersmaal = "Er barnet under 18 år på virkningstidspunktet?",
                    lovreferanse =
                        Lovreferanse(
                            paragraf = "§ 18-4",
                            ledd = 1,
                            lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-4",
                        ),
                ),
            unntaksvilkaar =
                listOf(
                    beggeForeldreDoedeUtdanningHovedbeskjeftigelse(),
                    beggeForeldreDoedeLaerlingPraktikantInntektUnder2G(),
                ),
        )

    private fun barnetsMedlemskap() =
        Vilkaar(
            hovedvilkaar =
                Delvilkaar(
                    type = VilkaarType.BP_FORTSATT_MEDLEMSKAP,
                    beskrivelse = "For å ha rett på ytelsen må barnet være medlem i trygden.",
                    spoersmaal = "Er barnet medlem i trygden på virkningstidspunktet?",
                    lovreferanse =
                        Lovreferanse(
                            paragraf = "§ 18-3",
                            ledd = 1,
                            lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-3",
                        ),
                ),
            unntaksvilkaar =
                listOf(
                    enForelderMinst20AarsSamletBotid(),
                    avdoedMindreEnn20AarsSamletBotidRettTilTilleggspensjon(),
                    barnetsMedlemskapYrkesskade(),
                    minstEttBarnForedreloestBarnekullMedlemTrygden(),
                ),
        )

    private fun vurderingAvEksport() =
        Vilkaar(
            hovedvilkaar =
                Delvilkaar(
                    type = VilkaarType.BP_VURDERING_AV_EKSPORT,
                    beskrivelse =
                        """
                        Barnepensjon kan eksporteres hvis en av foreldrene har minst 20 års samlet botid i Norge, hvis avdøde har mindre enn 20 års botid, men har opptjent rett til tilleggspensjon eller hvis minst ett av barna i et foreldreløst barnekull er medlem i trygden.

                        Skyldes dødsfallet en godkjent yrkesskade kan barnepensjonen eksporteres i sin helhet, jf. folketrygdloven § 18-10. Barnepensjon kan også fritt eksporteres til EØS-land, og til noen land Norge har bilaterale trygdeavtaler med. 

                        Barnepensjon etter unntaksbestemmelsene i § 18-2 tredje, fjerde og sjette ledd beholdes bare så lenge barnet er medlem i trygden. Den beholdes likevel hvis barnet bor i EØS-land, eller i et land Norge har trygdeavtale med der eksport er tillatt, eller hvis barnet er tredjelandsborger med rettigheter med hjemmel i C-55/00 Gottardo, jf. Rekommandasjon nr. H1 og forholdet omfattes av aktuell avtale. 

                        Andre hjemler:
                        EØS - rådsforordning 883/2004
                        Hovednummer 42 Trygdeavtaler
                        Lenke til C-55/00 Gottardo: https://lovdata.no/pro/#document/NAV/rundskriv/r45-00/KAPITTEL_1-10-3    
                        """.trimIndent(),
                    spoersmaal = "Kan barnepensjonen eksporteres?",
                    lovreferanse =
                        Lovreferanse(
                            paragraf = "§ 18-3",
                            ledd = 2,
                            lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-3",
                        ),
                ),
        )

    private fun avdoedesForutgaaendeMedlemskap() =
        Vilkaar(
            hovedvilkaar =
                Delvilkaar(
                    type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP,
                    beskrivelse =
                        """
                        For å ha rett på ytelsen må avdøde:
                        
                        a) ha vært medlem i trygden siste fem årene før dødsfallet, eller
                        b) ha mottatt pensjon eller uføretrygd siste fem årene før dødsfallet
                        """.trimIndent(),
                    spoersmaal = "Er et av vilkårene oppfylt?",
                    lovreferanse =
                        Lovreferanse(
                            paragraf = "§ 18-2",
                            ledd = 1,
                            lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-2",
                        ),
                ),
            unntaksvilkaar =
                listOf(
                    avdoedMedlemITrygdenIkkeFylt26Aar(),
                    avdoedMedlemEtter16AarMedUnntakAvMaksimum5Aar(),
                    avdoedMedlemVedDoedsfallKanTilstaaesHalvMinstepensjon(),
                    avdoedHaddeTidsromMedAvtalefestetPensjon(),
                    avdoedMedlemskapYrkesskade(),
                    avdoedHaddeTidsromMedPensjonFraLovfestetPensjonsordning(),
                ),
        )

    private fun avdoedesForutgaaendeMedlemskapEoes() =
        Vilkaar(
            hovedvilkaar =
                Delvilkaar(
                    type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_EOES,
                    beskrivelse =
                        """
                        Forutgående medlemskap kan være oppfylt ved sammenlegging av norsk trygdetid og trygdetid avdøde har opptjent fra EØS-land. Dette forutsetter at samlet trygdetid i Norge er minst ett år uten avrunding. Det er bare de avtalelandene der det er opparbeidet minst ett års trygdetid som skal tas med i sammenleggingen.
                        
                        Medlemskap i såkalte tredjeland som det er inngått bilaterale avtaler med kan også legges sammen med norsk trygdetid, forutsatt at avtalen omfatter pensjonsfordeler.
                         
                        Andre hjemler:
                        EØS - rådsforordning 1408/1971 artikkel 45 (gjelder perioder før 2004)
                        EØF - traktaten 1408/71 artikkel 39 (gjelder bilaterale avtaler)
                        Lenke: https://lovdata.no/pro/#document/DLX3/eu/31971r1408
                        """.trimIndent(),
                    spoersmaal = "Er forutgående medlemskap oppfylt ved sammenlegging?",
                    lovreferanse =
                        Lovreferanse(
                            paragraf = "EØS - rådsforordning 883/2004 artikkel 6 og 57",
                            lenke = "https://lovdata.no/pro/#document/NLX3/eu/32004r0883/ARTIKKEL_6",
                        ),
                ),
        )

    private fun yrkesskadeAvdoed() =
        Vilkaar(
            Delvilkaar(
                type = VilkaarType.BP_YRKESSKADE_AVDOED,
                beskrivelse =
                    """
                    Ved dødsfall som skyldes en skade eller sykdom som går inn under kapittel 13, ytes det barnepensjon etter følgende særbestemmelser:
                    
                    a) Vilkåret i § 18-2 om forutgående medlemskap gjelder ikke.
                    b) Vilkåret i § 18-3 om fortsatt medlemskap gjelder ikke.
                    c) Bestemmelsene i §18-5 om reduksjon på grunn av manglende trygdetid gjelder ikke.
                    d) Dersom barnet har utdanning som hovedbeskjeftigelse, ytes det pensjon inntil barnet fyller 21 år. 
                    e) Til praktikanter og lærlinger ytes det barnepensjon inntil barnet fyller 21 år, dersom arbeidsinntekten etter fradrag for skatt er mindre enn grunnbeløpet pluss særtillegg for enslige. 
                    """.trimIndent(),
                spoersmaal = "Skyldes dødsfallet en godkjent yrkesskade/sykdom?",
                lovreferanse =
                    Lovreferanse(
                        paragraf = "§ 18-11",
                        ledd = 1,
                        lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-11",
                    ),
            ),
        )

    private fun beggeForeldreDoedeUtdanningHovedbeskjeftigelse() =
        Delvilkaar(
            type = VilkaarType.BP_ALDER_BARN_UNNTAK_UTDANNING,
            lovreferanse =
                Lovreferanse(
                    paragraf = "§ 18-4",
                    ledd = 3,
                ),
        )

    private fun beggeForeldreDoedeLaerlingPraktikantInntektUnder2G() =
        Delvilkaar(
            type = VilkaarType.BP_ALDER_BARN_UNNTAK_LAERLING_PRAKTIKANT,
            lovreferanse =
                Lovreferanse(
                    paragraf = "§ 18-4",
                    ledd = 3,
                ),
        )

    private fun minstEttBarnForedreloestBarnekullMedlemTrygden() =
        Delvilkaar(
            type = VilkaarType.BP_FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRELOEST_BARN_I_KULL_MEDLEM_TRYGDEN,
            lovreferanse =
                Lovreferanse(
                    paragraf = "§ 18-3",
                    ledd = 2,
                    bokstav = "c",
                ),
        )

    private fun barnetsMedlemskapYrkesskade() =
        Delvilkaar(
            type = VilkaarType.BP_FORTSATT_MEDLEMSKAP_UNNTAK_YRKESSKADE,
            lovreferanse =
                Lovreferanse(
                    paragraf = "§ 18-11",
                    ledd = 1,
                    bokstav = "b",
                ),
        )

    private fun avdoedMindreEnn20AarsSamletBotidRettTilTilleggspensjon() =
        Delvilkaar(
            type = VilkaarType.BP_FORTSATT_MEDLEMSKAP_UNNTAK_AVDOED_MINDRE_20_AAR_BOTID_RETT_TILLEGGSPENSJON,
            lovreferanse =
                Lovreferanse(
                    paragraf = "§ 18-3",
                    ledd = 2,
                    bokstav = "b",
                ),
        )

    private fun enForelderMinst20AarsSamletBotid() =
        Delvilkaar(
            type = VilkaarType.BP_FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRE_MINST_20_AAR_SAMLET_BOTID,
            lovreferanse =
                Lovreferanse(
                    paragraf = "§ 18-3",
                    ledd = 2,
                    bokstav = "a",
                ),
        )

    private fun avdoedMedlemITrygdenIkkeFylt26Aar() =
        Delvilkaar(
            type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_IKKE_FYLT_26_AAR,
            lovreferanse =
                Lovreferanse(
                    paragraf = "§ 18-2",
                    ledd = 3,
                    bokstav = "a",
                ),
        )

    private fun avdoedMedlemEtter16AarMedUnntakAvMaksimum5Aar() =
        Delvilkaar(
            type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_MEDLEM_ETTER_16_AAR,
            lovreferanse =
                Lovreferanse(
                    paragraf = "§ 18-2",
                    ledd = 3,
                    bokstav = "b",
                ),
        )

    private fun avdoedMedlemVedDoedsfallKanTilstaaesHalvMinstepensjon() =
        Delvilkaar(
            type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_HALV_MINSTEPENSJON,
            lovreferanse =
                Lovreferanse(
                    paragraf = "§ 18-2",
                    ledd = 6,
                ),
        )

    private fun avdoedHaddeTidsromMedAvtalefestetPensjon() =
        Delvilkaar(
            type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_AVTALEFESTET_PENSJON,
            lovreferanse =
                Lovreferanse(
                    paragraf = "§ 18-2",
                    ledd = 5,
                ),
        )

    private fun avdoedHaddeTidsromMedPensjonFraLovfestetPensjonsordning() =
        Delvilkaar(
            type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_LOVFESTET_PENSJONSORDNING,
            lovreferanse =
                Lovreferanse(
                    paragraf = "§ 18-2",
                    ledd = 5,
                ),
        )

    private fun avdoedMedlemskapYrkesskade() =
        Delvilkaar(
            type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_YRKESSKADE,
            lovreferanse =
                Lovreferanse(
                    paragraf = "§ 18-11",
                    ledd = 1,
                    bokstav = "a",
                ),
        )
}
