@file:Suppress("DuplicatedCode")

package no.nav.etterlatte.vilkaarsvurdering.vilkaar

import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Delvilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Lovreferanse
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarOpplysningType.AVDOED_DOEDSDATO
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarOpplysningType.SOEKER_FOEDSELSDATO
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.vilkaarsvurdering.VilkaarFeatureToggle

object BarnepensjonVilkaar2024 {
    fun inngangsvilkaar(
        grunnlag: Grunnlag,
        featureToggleService: FeatureToggleService,
    ) = listOf(
        formaal(),
        doedsfallForelder(),
        yrkesskadeAvdoed(),
        alderBarn(grunnlag),
        barnetsMedlemskap(),
        vurderingAvEksport(),
        avdoedesForutgaaendeMedlemskap(),
    ).let { vilkaarListe ->
        val skalOppretteEoesVilkaar =
            featureToggleService.isEnabled(
                toggleId = VilkaarFeatureToggle.OpprettAvdoedesForutgaaendeMedlemskapEoesVilkaar,
                defaultValue = false,
            )

        if (skalOppretteEoesVilkaar) vilkaarListe.plus(avdoedesForutgaaendeMedlemskapEoes()) else vilkaarListe
    }

    private fun formaal() =
        Vilkaar(
            Delvilkaar(
                type = VilkaarType.BP_FORMAAL_2024,
                tittel = "Lever barnet?",
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
            Delvilkaar(
                type = VilkaarType.BP_DOEDSFALL_FORELDER_2024,
                tittel = "Dødsfall forelder",
                beskrivelse =
                    """
                    For å ha rett på ytelsen må en eller begge foreldre være registrer død i folkeregisteret eller hos utenlandske myndigheter.
                    """.trimIndent(),
                spoersmaal = "Er en eller begge foreldrene registrert som død?",
                lovreferanse =
                    Lovreferanse(
                        paragraf = "§ 18-1",
                        lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-1",
                    ),
            ),
        )

    private fun alderBarn(grunnlag: Grunnlag): Vilkaar =
        Vilkaar(
            hovedvilkaar =
                Delvilkaar(
                    type = VilkaarType.BP_ALDER_BARN_2024,
                    tittel = "Barnets alder",
                    beskrivelse =
                        """
                        For å ha rett på ytelsen må barnet være under 20 år på virkningstidspunktet.
                        
                        Det er overgangsregler for de som er innvilget barnepensjon før 01.01.2024, der dødsfall skyldtes en yrkes-skade/sykdom. Disse får pensjon til de er 21 år uavhengig av om de er under utdanning.
                        """.trimIndent(),
                    spoersmaal = "Er barnet under 20 år på virkningstidspunktet?",
                    lovreferanse =
                        Lovreferanse(
                            paragraf = "§ 18-4",
                            ledd = 1,
                            lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-4",
                        ),
                ),
            unntaksvilkaar = listOf(yrkesskadeFordelPaaGammeltRegelverk()),
            grunnlag =
                with(grunnlag) {
                    val foedselsdatoBarn = soeker.hentFoedselsdato()?.toVilkaarsgrunnlag(SOEKER_FOEDSELSDATO)
                    val doedsdatoAvdoede =
                        hentAvdoede().mapNotNull { avdoed ->
                            avdoed.hentDoedsdato()?.toVilkaarsgrunnlag(AVDOED_DOEDSDATO)
                        }
                    listOfNotNull(foedselsdatoBarn) + doedsdatoAvdoede
                },
        )

    private fun barnetsMedlemskap() =
        Vilkaar(
            hovedvilkaar =
                Delvilkaar(
                    type = VilkaarType.BP_FORTSATT_MEDLEMSKAP_2024,
                    tittel = "Barnets medlemskap",
                    beskrivelse =
                        """
                        For å ha rett på ytelsen må barnet være medlem i trygden.
                        
                        Det er unntak som gjør at vilkåret over ikke gjelder. Se hvilke når du krysser "Nei" til spørsmålet om barnet er medlem på virkningstidspunktet.
                        """.trimIndent(),
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
                    avdoedMindreEnn20AarsSamletBotidMinimumTrePoengaar(),
                    barnetsMedlemskapYrkesskade(),
                    minstEttBarnForedreloestBarnekullMedlemTrygden(),
                ),
        )

    private fun vurderingAvEksport() =
        Vilkaar(
            hovedvilkaar =
                Delvilkaar(
                    type = VilkaarType.BP_VURDERING_AV_EKSPORT_2024,
                    tittel = "Vurdering av eksport",
                    beskrivelse =
                        """
                        Barnepensjon kan eksporteres hvis en av foreldrene har minst 20 års samlet botid i Norge, hvis avdøde har mindre enn 20 års botid, men har minimum tre poengår eller hvis minst ett av barna i et foreldreløst barnekull er medlem i trygden.

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
                    type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_2024,
                    tittel = "Avdødes forutgående medlemskap - Folketrygden",
                    beskrivelse =
                        """
                        For å ha rett på ytelsen må avdøde:
                        a) ha vært medlem i trygden siste fem årene før dødsfallet, eller
                        b) ha mottatt pensjon eller uføretrygd siste fem årene før dødsfallet

                        Ved vurderingen av om a) er oppfylt, ses det bort fra perioder med tjeneste i internasjonale organisasjoner eller organer som staten Norge er medlem av, yter økonomisk bidrag til eller har ansvar for å bidra til bemanningen av.

                        Der er unntak som gjør at vilkårene over ikke gjelder. Se hvilke når du krysser "Nei" til spørsmålet om et av vilkårene er oppfylt.

                        Vilkåret er ikke oppfylt dersom den avdøde var arbeidsufør, men ikke hadde rett til uføretrygd fordi vilkåret om forutgående medlemskap i § 12-2 første ledd ikke var oppfylt.
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
                    type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_EOES_2024,
                    tittel = "Avdødes forutgående medlemskap - EØS/avtaleland",
                    beskrivelse =
                        """
                        Forutgående medlemskap kan være oppfylt ved sammenlegging av norsk trygdetid og trygdetid avdøde har opptjent fra EØS-land. Dette forutsetter at samlet trygdetid i Norge er minst ett år uten avrunding. 
                        Det er bare de avtalelandene der det er opparbeidet minst ett års trygdetid som skal tas med i sammenleggingen.
                        
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
                type = VilkaarType.BP_YRKESSKADE_AVDOED_2024,
                tittel = "Yrkesskade",
                beskrivelse =
                    """
                    Ved dødsfall som skyldes en skade eller sykdom som går inn under kapittel 13, ytes det barnepensjon etter følgende særbestemmelser:
                    
                    a) Vilkåret i § 18-2 om forutgående medlemskap gjelder ikke.
                    b) Vilkåret i § 18-3 om fortsatt medlemskap gjelder ikke.
                    c) Bestemmelsene i §18-5 om reduksjon på grunn av manglende trygdetid gjelder ikke.
                    """.trimIndent(),
                spoersmaal = "Skyldes dødsfallet en godkjent yrkesskade/sykdom?",
                lovreferanse =
                    Lovreferanse(
                        paragraf = "§ 18-10",
                        ledd = 1,
                        lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-10",
                    ),
            ),
        )

    private fun yrkesskadeFordelPaaGammeltRegelverk() =
        Delvilkaar(
            type = VilkaarType.BP_ALDER_BARN_UNNTAK_YS_GAMMELT_REGELVERK,
            tittel = "Ja. Bruker er innvilget barnepensjon før 01.01.2024 med yrkesskade-fordel og skal ha til fylte 21 år",
            lovreferanse =
                Lovreferanse(
                    paragraf = "§ 18-11",
                ),
        )

    private fun minstEttBarnForedreloestBarnekullMedlemTrygden() =
        Delvilkaar(
            type = VilkaarType.BP_FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRELOEST_BARN_I_KULL_MEDLEM_TRYGDEN_2024,
            tittel = "Ja. Minst ett av barna i et foreldreløst barnekull er medlem i trygden",
            lovreferanse =
                Lovreferanse(
                    paragraf = "§ 18-3",
                    ledd = 2,
                    bokstav = "c",
                ),
        )

    private fun barnetsMedlemskapYrkesskade() =
        Delvilkaar(
            type = VilkaarType.BP_FORTSATT_MEDLEMSKAP_UNNTAK_YRKESSKADE_2024,
            tittel = "Ja. Dødsfallet skyldes en godkjent yrkes-skade/sykdom",
            lovreferanse =
                Lovreferanse(
                    paragraf = "§ 18-11",
                    ledd = 1,
                    bokstav = "b",
                ),
        )

    private fun avdoedMindreEnn20AarsSamletBotidMinimumTrePoengaar() =
        Delvilkaar(
            type = VilkaarType.BP_FORTSATT_MEDLEMSKAP_UNNTAK_AVDOED_MINDRE_20_AAR_BOTID_TRE_POENGAAR_2024,
            tittel =
                """
                Ja. Den avdøde har mindre enn 20 års botid, men har minimum tre poengår. Trygdetid beregnes bare etter faktiske poengår
                """.trimIndent(),
            lovreferanse =
                Lovreferanse(
                    paragraf = "§ 18-3",
                    ledd = 2,
                    bokstav = "b",
                ),
        )

    private fun enForelderMinst20AarsSamletBotid() =
        Delvilkaar(
            type = VilkaarType.BP_FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRE_MINST_20_AAR_SAMLET_BOTID_2024,
            tittel = "Ja. En av foreldrene har minst 20 års samlet botid",
            lovreferanse =
                Lovreferanse(
                    paragraf = "§ 18-3",
                    ledd = 2,
                    bokstav = "a",
                ),
        )

    private fun avdoedMedlemITrygdenIkkeFylt26Aar() =
        Delvilkaar(
            type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_IKKE_FYLT_26_AAR_2024,
            tittel =
                """
                Ja. Avdøde var medlem ved dødsfallet og hadde ikke fylt 26 år (oppfylles tidligst fra tidspunktet avdøde ville ha vært medlem i ett år hvis dødsfallet ikke skjedde)
                """.trimIndent(),
            lovreferanse =
                Lovreferanse(
                    paragraf = "§ 18-2",
                    ledd = 3,
                    bokstav = "a",
                ),
        )

    private fun avdoedMedlemEtter16AarMedUnntakAvMaksimum5Aar() =
        Delvilkaar(
            type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_MEDLEM_ETTER_16_AAR_2024,
            tittel =
                """
                Ja. Avdøde var medlem ved dødsfallet og hadde vært medlem etter fylte 16 år med unntak av 5 år (oppfylles tidligst fra tidspunktet avdøde ville ha vært medlem i ett år hvis dødsfallet ikke skjedde)
                """.trimIndent(),
            lovreferanse =
                Lovreferanse(
                    paragraf = "§ 18-2",
                    ledd = 3,
                    bokstav = "b",
                ),
        )

    private fun avdoedMedlemVedDoedsfallKanTilstaaesHalvMinstepensjon() =
        Delvilkaar(
            type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_HALV_MINSTEPENSJON_2024,
            tittel =
                """
                Ja. Avdøde var medlem ved dødsfallet og kunne fått en ytelse på minst 1 G (har minst 20 års medlemskap, eller opptjent rett til tilleggspensjon høyere enn særtillegget)
                """.trimIndent(),
            lovreferanse =
                Lovreferanse(
                    paragraf = "§ 18-2",
                    ledd = 6,
                ),
        )

    private fun avdoedHaddeTidsromMedAvtalefestetPensjon() =
        Delvilkaar(
            type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_AVTALEFESTET_PENSJON_2024,
            tittel =
                """
                Ja. Avdøde hadde tidsrom med avtalefestet pensjon med statstilskott, som skal likestilles med tidsrom med pensjon fra folketrygden
                """.trimIndent(),
            lovreferanse =
                Lovreferanse(
                    paragraf = "§ 18-2",
                    ledd = 5,
                ),
        )

    private fun avdoedHaddeTidsromMedPensjonFraLovfestetPensjonsordning() =
        Delvilkaar(
            type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_LOVFESTET_PENSJONSORDNING_2024,
            tittel =
                """
                Ja. Avdøde hadde tidsrom med pensjon fra en lovfestet pensjonsordning som er tilpasset folketrygden ved at det ikke gis ordinær barnepensjon
                """.trimIndent(),
            lovreferanse =
                Lovreferanse(
                    paragraf = "§ 18-2",
                    ledd = 5,
                ),
        )

    private fun avdoedMedlemskapYrkesskade() =
        Delvilkaar(
            type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_YRKESSKADE_2024,
            tittel = "Ja. Dødsfallet skyldes en godkjent yrkes-skade/sykdom",
            lovreferanse =
                Lovreferanse(
                    paragraf = "§ 18-11",
                    ledd = 1,
                    bokstav = "a",
                ),
        )
}
