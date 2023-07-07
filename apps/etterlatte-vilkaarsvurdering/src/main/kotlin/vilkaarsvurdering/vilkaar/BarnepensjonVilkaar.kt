package no.nav.etterlatte.vilkaarsvurdering.vilkaar

import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Delvilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Lovreferanse
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarOpplysningType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarOpplysningType.AVDOED_DOEDSDATO
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarOpplysningType.SOEKER_FOEDSELSDATO
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsgrunnlag
import java.util.*

object BarnepensjonVilkaar {

    fun inngangsvilkaar(grunnlag: Grunnlag, virkningstidspunkt: Virkningstidspunkt) = listOf(
        formaal(),
        doedsfallForelder(),
        yrkesskadeAvdoed(),
        alderBarn(virkningstidspunkt, grunnlag),
        barnetsMedlemskap(),
        avdoedesForutgaaendeMedlemskap()
    )

    fun vilkaarForRevurdering(grunnlag: Grunnlag, revurderingAarsak: RevurderingAarsak): List<Vilkaar> =
        when (revurderingAarsak) {
            RevurderingAarsak.REGULERING,
            RevurderingAarsak.BARN,
            RevurderingAarsak.ANSVARLIGE_FORELDRE,
            RevurderingAarsak.NY_SOEKNAD,
            RevurderingAarsak.UTLAND,
            RevurderingAarsak.SOESKENJUSTERING -> emptyList()
            RevurderingAarsak.ADOPSJON -> listOf(doedsfallForelder())
            RevurderingAarsak.OMGJOERING_AV_FARSKAP -> listOf(doedsfallForelder())
            RevurderingAarsak.DOEDSFALL -> listOf(formaal())
            else -> throw Exception("Ugyldig revurderingsårsak $revurderingAarsak for behandling")
        }

    private fun formaal() = Vilkaar(
        Delvilkaar(
            type = VilkaarType.BP_FORMAAL,
            tittel = "Lever barnet?",
            beskrivelse = """
                Formålet med barnepensjon er å sikre inntekt for barn når en av foreldrene eller begge er døde. Dette betyr at barnet må være i live for å ha rett på barnepensjon.
            """.trimIndent(),
            spoersmaal = "Lever barnet det søkes barnepensjon for på virkningstidspunktet?",
            lovreferanse = Lovreferanse(
                paragraf = "§ 18-1",
                ledd = 1,
                lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-1"
            )
        )
    )

    private fun doedsfallForelder() = Vilkaar(
        Delvilkaar(
            type = VilkaarType.BP_DOEDSFALL_FORELDER,
            tittel = "Dødsfall forelder",
            beskrivelse = """
                For å ha rett på ytelsen må en eller begge foreldre være registrer død i folkeregisteret eller hos utenlandske myndigheter.
            """.trimIndent(),
            spoersmaal = "Er en eller begge foreldrene registrert som død?",
            lovreferanse = Lovreferanse(
                paragraf = "§ 18-4",
                ledd = 2,
                lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-4"
            )
        )
    )

    private fun alderBarn(
        virkningstidspunkt: Virkningstidspunkt,
        grunnlag: Grunnlag
    ): Vilkaar = Vilkaar(
        hovedvilkaar = Delvilkaar(
            type = VilkaarType.BP_ALDER_BARN,
            tittel = "Barnets alder",
            beskrivelse = "For å ha rett på ytelsen må barnet være under 18 år på virkningstidspunktet.",
            spoersmaal = "Er barnet under 18 år på virkningstidspunktet?",
            lovreferanse = Lovreferanse(
                paragraf = "§ 18-4",
                ledd = 1,
                lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-4"
            )
        ),
        unntaksvilkaar = listOf(
            beggeForeldreDoedeUtdanningHovedbeskjeftigelse(),
            beggeForeldreDoedeLaerlingPraktikantInntektUnder2G()
        ),
        grunnlag = with(grunnlag) {
            /**
             * EY-1561: Fjerner virkningstidspunkt fra grunnlaget siden vi ikke har kontroll på om virk. har endret seg.
             * val virkningstidspunktBehandling = virkningstidspunkt.toVilkaarsgrunnlag()
             */
            val foedselsdatoBarn = soeker.hentFoedselsdato()?.toVilkaarsgrunnlag(SOEKER_FOEDSELSDATO)
            val doedsdatoAvdoed = hentAvdoed().hentDoedsdato()?.toVilkaarsgrunnlag(AVDOED_DOEDSDATO)

            listOfNotNull(foedselsdatoBarn, doedsdatoAvdoed /*, virkningstidspunktBehandling*/)
        }
    )

    private fun barnetsMedlemskap() = Vilkaar(
        hovedvilkaar = Delvilkaar(
            type = VilkaarType.BP_FORTSATT_MEDLEMSKAP,
            tittel = "Barnets medlemskap",
            beskrivelse = "For å ha rett på ytelsen må barnet være medlem i trygden.",
            spoersmaal = "Er barnet medlem i trygden på virkningstidspunktet?",
            lovreferanse = Lovreferanse(
                paragraf = "§ 18-3",
                ledd = 1,
                lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-3"
            )
        ),
        unntaksvilkaar = listOf(
            enForelderMinst20AarsSamletBotid(),
            avdoedMindreEnn20AarsSamletBotidRettTilTilleggspensjon(),
            minstEttBarnForedreloestBarnekullMedlemTrygden()
        )
    )

    private fun avdoedesForutgaaendeMedlemskap() = Vilkaar(
        hovedvilkaar = Delvilkaar(
            type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP,
            tittel = "Avdødes forutgående medlemskap",
            beskrivelse = """
                For å ha rett på ytelsen må avdøde:
                
                a) ha vært medlem i trygden siste fem årene før dødsfallet, eller
                b) ha mottatt pensjon eller uføretrygd siste fem årene før dødsfallet
            """.trimIndent(),
            spoersmaal = "Er et av vilkårene oppfylt?",
            lovreferanse = Lovreferanse(
                paragraf = "§ 18-2",
                ledd = 1,
                lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-2"
            )
        ),
        unntaksvilkaar = listOf(
            avdoedMedlemITrygdenIkkeFylt26Aar(),
            avdoedMedlemEtter16AarMedUnntakAvMaksimum5Aar(),
            avdoedMedlemVedDoedsfallKanTilstaaesHalvMinstepensjon(),
            avdoedHaddeTidsromMedAvtalefestetPensjon(),
            avdoedHaddeTidsromMedPensjonFraLovfestetPensjonsordning()
        )
    )

    private fun yrkesskadeAvdoed() = Vilkaar(
        Delvilkaar(
            type = VilkaarType.BP_YRKESSKADE_AVDOED,
            tittel = "Yrkesskade",
            beskrivelse = """
                Ved dødsfall som skyldes en skade eller sykdom som går inn under kapittel 13, ytes det barnepensjon etter følgende særbestemmelser:
                
                a) Vilkåret i § 18-2 om forutgående medlemskap gjelder ikke.
                b) Vilkåret i § 18-3 om fortsatt medlemskap gjelder ikke.
                c) Bestemmelsene i §18-5 om reduksjon på grunn av manglende trygdetid gjelder ikke.
                d) Dersom barnet har utdanning som hovedbeskjeftigelse, ytes det pensjon inntil barnet fyller 21 år. 
                e) Til praktikanter og lærlinger ytes det barnepensjon inntil barnet fyller 21 år, dersom arbeidsinntekten etter fradrag for skatt er mindre enn grunnbeløpet pluss særtillegg for enslige. 
            """.trimIndent(),
            spoersmaal = "Skyldes dødsfallet en godkjent yrkesskade/sykdom?",
            lovreferanse = Lovreferanse(
                paragraf = "§ 18-11",
                ledd = 1,
                lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-11"
            )
        )
    )

    private fun beggeForeldreDoedeUtdanningHovedbeskjeftigelse() = Delvilkaar(
        type = VilkaarType.BP_ALDER_BARN_UNNTAK_UTDANNING,
        tittel = "Ja. Barnet er foreldreløs og har utdanning som hovedbeskjeftigelse",
        lovreferanse = Lovreferanse(
            paragraf = "§ 18-4",
            ledd = 3
        )
    )

    private fun beggeForeldreDoedeLaerlingPraktikantInntektUnder2G() = Delvilkaar(
        type = VilkaarType.BP_ALDER_BARN_UNNTAK_LAERLING_PRAKTIKANT,
        tittel = """
            Ja. Barnet er foreldreløs og er lærling/praktikant med en inntekt etter skatt på mindre enn to ganger grunnbeløpet
        """.trimIndent(),
        lovreferanse = Lovreferanse(
            paragraf = "§ 18-4",
            ledd = 3
        )
    )

    private fun minstEttBarnForedreloestBarnekullMedlemTrygden() = Delvilkaar(
        type = VilkaarType.BP_FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRELOEST_BARN_I_KULL_MEDLEM_TRYGDEN,
        tittel = "Ja. Minst ett av barna i et foreldreløst barnekull er medlem i trygden",
        lovreferanse = Lovreferanse(
            paragraf = "§ 18-3",
            ledd = 2,
            bokstav = "c"
        )
    )

    private fun avdoedMindreEnn20AarsSamletBotidRettTilTilleggspensjon() = Delvilkaar(
        type = VilkaarType.BP_FORTSATT_MEDLEMSKAP_UNNTAK_AVDOED_MINDRE_20_AAR_BOTID_RETT_TILLEGGSPENSJON,
        tittel = "Ja. Den avdøde har mindre enn 20 års botid, men har opptjent rett til tilleggspensjon",
        lovreferanse = Lovreferanse(
            paragraf = "§ 18-3",
            ledd = 2,
            bokstav = "b"
        )
    )

    private fun enForelderMinst20AarsSamletBotid() = Delvilkaar(
        type = VilkaarType.BP_FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRE_MINST_20_AAR_SAMLET_BOTID,
        tittel = "Ja. En av foreldrene har minst 20 års samlet botid",
        lovreferanse = Lovreferanse(
            paragraf = "§ 18-3",
            ledd = 2,
            bokstav = "a"
        )
    )

    private fun avdoedMedlemITrygdenIkkeFylt26Aar() = Delvilkaar(
        type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_IKKE_FYLT_26_AAR,
        tittel = """
            Ja. Avdøde var medlem ved dødsfallet og hadde ikke fylt 26 år (oppfylles tidligst fra tidspunktet avdøde ville ha vært medlem i ett år hvis dødsfallet ikke skjedde
        """.trimIndent(),
        lovreferanse = Lovreferanse(
            paragraf = "§ 18-2",
            ledd = 3,
            bokstav = "a"
        )
    )

    private fun avdoedMedlemEtter16AarMedUnntakAvMaksimum5Aar() = Delvilkaar(
        type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_MEDLEM_ETTER_16_AAR,
        tittel = """
            Ja. Avdøde var medlem ved dødsfallet og hadde vært medlem etter fylte 16 år med unntak av 5 år (oppfylles tidligst fra tidspunktet avdøde ville ha vært medlem i ett år hvis dødsfallet ikke skjedde)
        """.trimIndent(),
        lovreferanse = Lovreferanse(
            paragraf = "§ 18-2",
            ledd = 3,
            bokstav = "b"
        )
    )

    private fun avdoedMedlemVedDoedsfallKanTilstaaesHalvMinstepensjon() = Delvilkaar(
        type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_HALV_MINSTEPENSJON,
        tittel = """
            Ja. Avdøde var medlem ved dødsfallet og kunne fått en ytelse på minst 1 G (har minst 20 års medlemskap, eller opptjent rett til tilleggspensjon høyere enn særtillegget)
        """.trimIndent(),
        lovreferanse = Lovreferanse(
            paragraf = "§ 18-2",
            ledd = 6
        )
    )

    private fun avdoedHaddeTidsromMedAvtalefestetPensjon() = Delvilkaar(
        type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_AVTALEFESTET_PENSJON,
        tittel = """
            Ja. Avdøde hadde tidsrom med avtalefestet pensjon med statstilskott, som skal likestilles med tidsrom med pensjon fra folketrygden
        """.trimIndent(),
        lovreferanse = Lovreferanse(
            paragraf = "§ 18-2",
            ledd = 5
        )
    )

    private fun avdoedHaddeTidsromMedPensjonFraLovfestetPensjonsordning() = Delvilkaar(
        type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_LOVFESTET_PENSJONSORDNING,
        tittel = """
            Ja. Avdøde hadde tidsrom med pensjon fra en lovfestet pensjonsordning som er tilpasset folketrygden ved at det ikke gis ordinær barnepensjon
        """.trimIndent(),
        lovreferanse = Lovreferanse(
            paragraf = "§ 18-2",
            ledd = 5
        )
    )

    internal fun <T> Opplysning.Konstant<out T?>.toVilkaarsgrunnlag(type: VilkaarOpplysningType) =
        Vilkaarsgrunnlag(
            id = id,
            opplysningsType = type,
            kilde = kilde,
            opplysning = verdi
        )

    private fun Virkningstidspunkt.toVilkaarsgrunnlag() =
        Vilkaarsgrunnlag(
            id = UUID.randomUUID(),
            opplysningsType = VilkaarOpplysningType.VIRKNINGSTIDSPUNKT,
            kilde = kilde,
            opplysning = dato
        )
}