package no.nav.etterlatte.vilkaarsvurdering.vilkaar

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
        doedsfallForelder(),
        alderBarn(virkningstidspunkt, grunnlag),
        barnetsMedlemskap(),
        avdoedesForutgaaendeMedlemskap(),
        yrkesskadeAvdoed()
    )

    fun loependevilkaar() = listOf(
        formaal()
    )

    private fun formaal() = Vilkaar(
        Delvilkaar(
            type = VilkaarType.BP_FORMAAL,
            tittel = "Formål",
            beskrivelse =
            "Formålet med barnepensjon er å sikre inntekt for barn når en av foreldrene eller begge er døde.",
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
            beskrivelse = "En eller begge foreldrene er registrert død",
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
            beskrivelse = "Barnet er under 18 år (på virkningstidspunkt)",
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
            beskrivelse = "Barnet er medlem i trygden (fra virkningstidspunkt)",
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
            beskrivelse =
            "Avdød har vært medlem eller mottatt pensjon/uføretrygd fra folketrygden de " +
                "siste fem årene fram til dødsfallet",
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
            beskrivelse = "Dødsfallet skyldes en godkjent yrkes-skade/sykdom",
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
        tittel =
        "Ja. Barnet er foreldreløs og er lærling/praktikant med en inntekt etter skatt på mindre enn to " +
            "ganger grunnbeløpet",
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
        tittel =
        "Ja. Avdøde var medlem ved dødsfallet og hadde ikke fylt 26 år (oppfylles tidligst fra tidspunktet " +
            "avdøde ville ha vært medlem i ett år hvis dødsfallet ikke skjedde",
        lovreferanse = Lovreferanse(
            paragraf = "§ 18-2",
            ledd = 3,
            bokstav = "a"
        )
    )

    private fun avdoedMedlemEtter16AarMedUnntakAvMaksimum5Aar() = Delvilkaar(
        type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_MEDLEM_ETTER_16_AAR,
        tittel =
        "Ja. Avdøde var medlem ved dødsfallet og hadde vært medlem etter fylte 16 år med unntak av 5 år " +
            "(oppfylles tidligst fra tidspunktet avdøde ville ha vært medlem i ett år hvis dødsfallet ikke skjedde)",
        lovreferanse = Lovreferanse(
            paragraf = "§ 18-2",
            ledd = 3,
            bokstav = "b"
        )
    )

    private fun avdoedMedlemVedDoedsfallKanTilstaaesHalvMinstepensjon() = Delvilkaar(
        type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_HALV_MINSTEPENSJON,
        tittel =
        "Ja. Avdøde var medlem ved dødsfallet og kunne fått en ytelse på minst 1 G (har minst 20 års " +
            "medlemskap, eller opptjent rett til tilleggspensjon høyere enn særtillegget)",
        lovreferanse = Lovreferanse(
            paragraf = "§ 18-2",
            ledd = 6
        )
    )

    private fun avdoedHaddeTidsromMedAvtalefestetPensjon() = Delvilkaar(
        type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_AVTALEFESTET_PENSJON,
        tittel =
        "Ja. Avdøde hadde tidsrom med avtalefestet pensjon med statstilskott, som skal likestilles med tidsrom " +
            "med pensjon fra folketrygden",
        lovreferanse = Lovreferanse(
            paragraf = "§ 18-2",
            ledd = 5
        )
    )

    private fun avdoedHaddeTidsromMedPensjonFraLovfestetPensjonsordning() = Delvilkaar(
        type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_LOVFESTET_PENSJONSORDNING,
        tittel =
        "Ja. Avdøde hadde tidsrom med pensjon fra en lovfestet pensjonsordning som er tilpasset " +
            "folketrygden ved at det ikke gis ordinær barnepensjon",
        lovreferanse = Lovreferanse(
            paragraf = "§ 18-2",
            ledd = 5
        )
    )

    private fun <T> Opplysning.Konstant<out T?>.toVilkaarsgrunnlag(type: VilkaarOpplysningType) =
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