package no.nav.etterlatte.vilkaarsvurdering.vilkaar

import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentSoeknadMottattDato
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Delvilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Lovreferanse
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarOpplysningType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.BarnepensjonVilkaar.toVilkaarsgrunnlag

object OmstillingstoenadVilkaar {

    fun inngangsvilkaar(grunnlag: Grunnlag) = listOf(
        etterlatteLever(),
        doedsfall(),
        etterlatteSivilstand(),
        yrkesskade(),
        avdoedesMedlemskap(),
        gjenlevendesMedlemskap(),
        aktivitetEtter6Maaneder(grunnlag),
        oevrigeVilkaar()
    )

    private fun etterlatteLever() = Vilkaar(
        hovedvilkaar = Delvilkaar(
            type = VilkaarType.OMS_ETTERLATTE_LEVER,
            tittel = "Lever den etterlatte?",
            beskrivelse = """
                Formålet med omstillingsstønad er blant annet å sikre inntekt til ektefelle/partner/samboer/tidligere familiepleier. Dette betyr at den etterlatte må være i live for å ha rett til stønaden.
            """.trimIndent(),
            spoersmaal = "Lever den etterlatte på virkningstidspunktet?",
            lovreferanse = Lovreferanse(
                paragraf = "§ 17-1"
            )
        )
    )

    private fun doedsfall() = Vilkaar(
        hovedvilkaar = Delvilkaar(
            type = VilkaarType.OMS_DOEDSFALL,
            tittel = "Dødsfall ektefelle/partner/samboer",
            beskrivelse = """
                For at dette vilkåret skal være oppfylt må dødsfallet gjelde ektefelle/partner/samboer, og det må være registrert i folkeregisteret eller hos utenlandske trygdemyndigheter. 
            """.trimIndent(),
            spoersmaal = "Er ektefelle/partner/samboer registrert død?",
            lovreferanse = Lovreferanse(
                paragraf = "§ 17-1"
            )
        )
    )

    private fun etterlatteSivilstand() = Vilkaar(
        hovedvilkaar = Delvilkaar(
            type = VilkaarType.OMS_SIVILSTAND,
            tittel = "Har etterlatte rett til omstillingsstønad etter § 17-11? ",
            beskrivelse = """
                Retten til omstillingsstønad faller bort når etterlatte fyller 67 år eller tar ut alderspensjon, får rett til 100 % uføretrygd, mottar AFP fra en offentlig pensjonsordning eller gifter seg igjen, gjelder også samboere etter § 1-5.
                
                Hvis en etterlatt som er innvilget omstillingsstønad etter § 17-5 tredje ledd inngår skilsmisse innen to år, får vedkommende rett til omstillingsstønad igjen.
            """.trimIndent(),
            spoersmaal = "Er vilkår oppfylt?",
            lovreferanse = Lovreferanse(
                paragraf = "§ 17-11"
            )
        )
    )

    private fun yrkesskade() = Vilkaar(
        hovedvilkaar = Delvilkaar(
            type = VilkaarType.OMS_YRKESSKADE,
            tittel = "Dødsfall som skyldes yrkesskade eller yrkessykdom",
            beskrivelse = """
                Ved dødsfall som skyldes en skade eller sykdom som går inn under kapittel 13, ytes det omstillingsstønad til gjenlevende ektefelle etter følgende særbestemmelser:
                    
                a) Vilkåret i § 17-2 om forutgående medlemskap gjelder ikke.
                b) Vilkåret i § 17-3 om fortsatt medlemskap gjelder ikke.
                c) Vilkåret i § 17-4 om ekteskapets varighet gjelder ikke.
                    
                Omstillingsstønaden avkortes ikke på grunn av redusert trygdetid.
            """.trimIndent(),
            spoersmaal = "Skyldes dødsfallet en godkjent yrkesskade/sykdom?",
            lovreferanse = Lovreferanse(
                paragraf = "§ 17-12"
            )
        )
    )

    private fun avdoedesMedlemskap() = Vilkaar(
        hovedvilkaar = Delvilkaar(
            type = VilkaarType.OMS_AVDOEDES_MEDLEMSKAP,
            tittel = "Avdødes forutgående medlemskap",
            beskrivelse = """
                For at dette vilkåret skal være oppfylt, og gjenlevende ektefelle har rett til ytelser etter dette kapitlet, må den avdøde:

                a) ha vært medlem i trygden siste fem årene før dødsfallet, eller
                b) ha mottatt pensjon eller uføretrygd siste fem årene før dødsfallet
            """.trimIndent(),
            spoersmaal = "Er et av vilkårene oppfylt?",
            lovreferanse = Lovreferanse(
                paragraf = "§ 17-2"
            )
        ),
        unntaksvilkaar = listOf(
            avdoedesMedlemskapUnder26(),
            avdoedesMedlemskapOver16(),
            avdoedesMedlemskapPensjon(),
            avdoedesMedlemskapYrkesskade(),
            avdoedesMedlemskapOpptjening()
        )
    )

    private fun avdoedesMedlemskapOpptjening() = Delvilkaar(
        type = VilkaarType.OMS_AVDOEDES_MEDLEMSKAP_UNNTAK_OPPTJENING,
        tittel = "Ja, avdøde kunne tilstås en ytelse på grunnlag av tidligere opptjening",
        lovreferanse = Lovreferanse(
            paragraf = "§ 17-2" // TODO
        )
    )

    private fun avdoedesMedlemskapPensjon() = Delvilkaar(
        type = VilkaarType.OMS_AVDOEDES_MEDLEMSKAP_UNNTAK_PENSJON,
        tittel = "Ja, avdøde hadde avtalefestet pensjon, se femte ledd",
        lovreferanse = Lovreferanse(
            paragraf = "§ 17-2" // TODO
        )
    )

    private fun avdoedesMedlemskapOver16() = Delvilkaar(
        type = VilkaarType.OMS_AVDOEDES_MEDLEMSKAP_UNNTAK_OVER_16,
        tittel = """
            Ja, avdøde var medlem av trygden ved dødsfallet og hadde vært medlem etter fylte 16 år med unntak av 5 år
        """.trimIndent(),
        lovreferanse = Lovreferanse(
            paragraf = "§ 17-2" // TODO
        )
    )

    private fun avdoedesMedlemskapUnder26() = Delvilkaar(
        type = VilkaarType.OMS_AVDOEDES_MEDLEMSKAP_UNNTAK_UNDER_26,
        tittel = "Ja, avdøde var medlem av trygden ved dødsfallet og ikke fylt 26 år",
        lovreferanse = Lovreferanse(
            paragraf = "§ 17-2" // TODO
        )
    )

    private fun avdoedesMedlemskapYrkesskade() = Delvilkaar(
        type = VilkaarType.OMS_AVDOEDES_MEDLEMSKAP_UNNTAK_YRKESSKADE,
        tittel = "Ja, dødsfallet skyldes en godkjent yrkes-skade/sykdom",
        lovreferanse = Lovreferanse(
            paragraf = "§ 17-2" // TODO
        )
    )

    private fun gjenlevendesMedlemskap() = Vilkaar(
        hovedvilkaar = Delvilkaar(
            type = VilkaarType.OMS_GJENLEVENDES_MEDLEMSKAP,
            tittel = "Gjenlevendes medlemskap",
            beskrivelse = """
                For at dette vilkåret skal være oppfylt, og gjenlevende ha rett til ytelser etter dette kapitlet, må vedkommende være medlem i trygden.
            """.trimIndent(),
            spoersmaal = "Er gjenlevende medlem i trygden?",
            lovreferanse = Lovreferanse(
                paragraf = "§ 17-3"
            )
        ),
        unntaksvilkaar = listOf(
            gjenlevendesMedlemskapBotid(),
            gjenlevendesMedlemskapYrkesskade(),
            gjenlevendesMedlemskapPensjon()
        )
    )

    private fun gjenlevendesMedlemskapPensjon() = Delvilkaar(
        type = VilkaarType.OMS_GJENLEVENDES_MEDLEMSKAP_UNNTAK_PENSJON,
        tittel = """
            Ja, både den avdøde og den gjenlevende har mindre enn 20 års botid, men avdøde har opptjent rett til tilleggspensjon
        """.trimIndent(),
        lovreferanse = Lovreferanse(
            paragraf = "§ 17-3" // TODO
        )
    )

    private fun gjenlevendesMedlemskapBotid() = Delvilkaar(
        type = VilkaarType.OMS_GJENLEVENDES_MEDLEMSKAP_UNNTAK_BOTID,
        tittel = "Ja, den avdøde eller den gjenlevende har minst 20 års samlet botid",
        lovreferanse = Lovreferanse(
            paragraf = "§ 17-3" // TODO
        )
    )

    private fun gjenlevendesMedlemskapYrkesskade() = Delvilkaar(
        type = VilkaarType.OMS_GJENLEVENDES_MEDLEMSKAP_UNNTAK_YRKESSKADE,
        tittel = "Ja, dødsfallet skyldes en godkjent yrkes-skade/sykdom",
        lovreferanse = Lovreferanse(
            paragraf = "§ 17-2" // TODO
        )
    )

    private fun oevrigeVilkaar() = Vilkaar(
        hovedvilkaar = Delvilkaar(
            type = VilkaarType.OMS_OEVRIGE_VILKAAR,
            tittel = "Øvrige vilkår for ytelser",
            beskrivelse = """
                For at dette vilkåret skal være oppfylt, og gjenlevende ektefelle ha rett til ytelser etter dette kapitlet, må ett av disse vilkårene være oppfylt på tidspunktet for dødsfallet:
                
                a) var gift med den avdøde og ekteskapet hadde vart i minst fem år,
                b) har eller har hatt barn med den avdøde eller
                c) har omsorg for barn under 18 år med minst halvparten av full tid.
                
                Til en fraskilt person som helt eller i det vesentlige har vært forsørget av bidrag fra den avdøde, kan det ytes stønad etter dette kapitlet dersom ekteskapet varte i minst 25 år, eller minst 15 år hvis ektefellene hadde barn sammen.
            """.trimIndent(),
            spoersmaal = "Er et av vilkårene oppfylt?",
            lovreferanse = Lovreferanse(
                paragraf = "§ 17-4"
            )
        )
    )

    private fun aktivitetEtter6Maaneder(grunnlag: Grunnlag) = Vilkaar(
        hovedvilkaar = Delvilkaar(
            type = VilkaarType.OMS_AKTIVITET_ETTER_6_MND,
            tittel = "Krav til aktivitet etter 6 måneder",
            beskrivelse = """
                Seks måneder etter dødsfallet er det et vilkår for rett til omstillingsstønad at den gjenlevende:

                a) er i minst 50 % arbeid,
                b) er reell arbeidssøker,
                c) gjennomfører nødvendig og hensiktsmessig opplæring eller utdanning, minst 50 %, eller
                d) etablerer egen virksomhet

                Det finnes unntak som gir fritak for aktivitetskravet, som må vurderes om ikke hovedvilkåret er oppfylt.
            """.trimIndent(),
            spoersmaal = "Er vilkåret om krav til aktivitet oppfylt?",
            lovreferanse = Lovreferanse(
                paragraf = "§ 17-7"
            )
        ),
        unntaksvilkaar = listOf(
            aktivitetEtter6MaanederGjenlevendeOver55ogLavInntekt(),
            aktivitetEtter6MaanederGjenlevendeHarBarnUnder1Aar()
        ),
        grunnlag = with(grunnlag) {
            val doedsdatoAvdoedGrunnlag = hentAvdoed().hentDoedsdato()?.toVilkaarsgrunnlag(
                VilkaarOpplysningType.AVDOED_DOEDSDATO
            )
            val soeknadMottattDatoGrunnlag = sak.hentSoeknadMottattDato()?.toVilkaarsgrunnlag(
                VilkaarOpplysningType.SOEKNAD_MOTTATT_DATO
            )
            listOfNotNull(doedsdatoAvdoedGrunnlag, soeknadMottattDatoGrunnlag)
        }
    )

    private fun aktivitetEtter6MaanederGjenlevendeOver55ogLavInntekt() = Delvilkaar(
        type = VilkaarType.OMS_AKTIVITET_ETTER_6_MND_UNNTAK_GJENLEVENDE_OVER_55_AAR_OG_LAV_INNTEKT,
        tittel = "Ja, gjenlevende er over 55 år ved dødsfall og har hatt lav inntekt",
        lovreferanse = Lovreferanse(
            paragraf = "§ 17-7"
        )
    )

    private fun aktivitetEtter6MaanederGjenlevendeHarBarnUnder1Aar() = Delvilkaar(
        type = VilkaarType.OMS_AKTIVITET_ETTER_6_MND_UNNTAK_GJENLEVENDE_BARN_UNDER_1_AAR,
        tittel = "Ja, gjenlevende har barn som er under 1 år",
        lovreferanse = Lovreferanse(
            paragraf = "§ 17-7"
        )
    )
}