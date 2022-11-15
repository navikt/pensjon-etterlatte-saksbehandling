package no.nav.etterlatte.vilkaarsvurdering.barnepensjon

import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Hovedvilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Paragraf
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Unntaksvilkaar
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
        Hovedvilkaar(
            type = VilkaarType.FORMAAL,
            tittel = "Formål",
            beskrivelse =
            "Formålet med barnepensjon er å sikre inntekt for barn når en av foreldrene eller begge er døde.",
            lovreferanse = Paragraf(
                paragraf = "§ 18-1",
                ledd = 1,
                lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-1"
            )
        )
    )

    private fun doedsfallForelder() = Vilkaar(
        Hovedvilkaar(
            type = VilkaarType.DOEDSFALL_FORELDER,
            tittel = "Dødsfall forelder",
            beskrivelse = "En eller begge foreldrene er registrert død",
            lovreferanse = Paragraf(
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
        hovedvilkaar = Hovedvilkaar(
            type = VilkaarType.ALDER_BARN,
            tittel = "Barnets alder",
            beskrivelse = "Barnet er under 18 år (på virkningstidspunkt)",
            lovreferanse = Paragraf(
                paragraf = "§ 18-4",
                ledd = 1,
                lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-4"
            )
        ),
        unntaksvilkaar = listOf(
            beggeForeldreDoedeUtdanningHovedbeskjeftigelse()
        ),
        grunnlag = with(grunnlag) {
            val virkningstidspunktBehandling = virkningstidspunkt.toVilkaarsgrunnlag()
            val foedselsdatoBarn = soeker.hentFoedselsdato()?.toVilkaarsgrunnlag(SOEKER_FOEDSELSDATO)
            val doedsdatoAvdoed = hentAvdoed().hentDoedsdato()?.toVilkaarsgrunnlag(AVDOED_DOEDSDATO)

            listOfNotNull(foedselsdatoBarn, doedsdatoAvdoed, virkningstidspunktBehandling)
        }
    )

    private fun barnetsMedlemskap() = Vilkaar(
        hovedvilkaar = Hovedvilkaar(
            type = VilkaarType.FORTSATT_MEDLEMSKAP,
            tittel = "Barnets medlemskap",
            beskrivelse = "Barnet er medlem i trygden (fra virkningstidspunkt)",
            lovreferanse = Paragraf(
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
        hovedvilkaar = Hovedvilkaar(
            type = VilkaarType.FORUTGAAENDE_MEDLEMSKAP,
            tittel = "Avdødes forutgående medlemskap",
            beskrivelse =
            "Avdød har vært medlem eller mottatt pensjon/uføretrygd fra folketrygden de " +
                "siste fem årene fram til dødsfallet",
            lovreferanse = Paragraf(
                paragraf = "§ 18-2",
                ledd = 1,
                lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-2"
            )
        ),
        unntaksvilkaar = listOf(
            avdoedMedlemITrygdenIkkeFylt26Aar(),
            avdoedMedlemEtter16AarMedUnntakAvMaksimum5Aar(),
            avdoedMedlemVedDoedsfallKanTilstaaesHalvMinstepensjon()
        )
    )

    private fun yrkesskadeAvdoed() = Vilkaar(
        Hovedvilkaar(
            type = VilkaarType.YRKESSKADE_AVDOED,
            tittel = "Yrkesskade",
            beskrivelse = "Dødsfallet skyldes en godkjent yrkes-skade/sykdom",
            lovreferanse = Paragraf(
                paragraf = "§ 18-11",
                ledd = 1,
                lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-11"
            )
        )
    )

    private fun beggeForeldreDoedeUtdanningHovedbeskjeftigelse() = Unntaksvilkaar(
        type = VilkaarType.ALDER_BARN_UNNTAK_UTDANNING,
        tittel = "Begge foreldrene er døde og barnet har utdanning som hovedbeskjeftigelse",
        lovreferanse = Paragraf(
            paragraf = "§ 18-4",
            ledd = 3
        )
    )

    private fun minstEttBarnForedreloestBarnekullMedlemTrygden() = Unntaksvilkaar(
        type = VilkaarType.FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRELOEST_BARN_I_KULL_MEDLEM_TRYGDEN,
        tittel = "Minst ett av barna i et foreldreløst barnekull er medlem i trygden",
        lovreferanse = Paragraf(
            paragraf = "§ 18-3",
            ledd = 2,
            bokstav = "c"
        )
    )

    private fun avdoedMindreEnn20AarsSamletBotidRettTilTilleggspensjon() = Unntaksvilkaar(
        type = VilkaarType.FORTSATT_MEDLEMSKAP_UNNTAK_AVDOED_MINDRE_20_AAR_BOTID_RETT_TILLEGGSPENSJON,
        tittel = "Minst ett av barna i et foreldreløst barnekull er medlem i trygden",
        lovreferanse = Paragraf(
            paragraf = "§ 18-3",
            ledd = 2,
            bokstav = "b"
        )
    )

    private fun enForelderMinst20AarsSamletBotid() = Unntaksvilkaar(
        type = VilkaarType.FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRE_MINST_20_AAR_SAMLET_BOTID,
        tittel = "En av foreldrene har minst 20 års samlet botid",
        lovreferanse = Paragraf(
            paragraf = "§ 18-3",
            ledd = 2,
            bokstav = "a"
        )
    )

    private fun avdoedMedlemITrygdenIkkeFylt26Aar() = Unntaksvilkaar(
        type = VilkaarType.FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_IKKE_FYLT_26_AAR,
        tittel = "Avdøde medlem av trygden ved dødsfallet og ikke fylt 26 år",
        lovreferanse = Paragraf(
            paragraf = "§ 18-2",
            ledd = 3,
            bokstav = "a"
        )
    )

    private fun avdoedMedlemEtter16AarMedUnntakAvMaksimum5Aar() = Unntaksvilkaar(
        type = VilkaarType.FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_MEDLEM_ETTER_16_AAR,
        tittel = "Avdøde medlem av trygden ved dødsfallet og hadde vært medlem etter fylte 16 år med unntak av 5 år",
        lovreferanse = Paragraf(
            paragraf = "§ 18-2",
            ledd = 3,
            bokstav = "b"
        )
    )

    private fun avdoedMedlemVedDoedsfallKanTilstaaesHalvMinstepensjon() = Unntaksvilkaar(
        type = VilkaarType.FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_HALV_MINSTEPENSJON,
        tittel = "Avdøde kunne tilstås en ytelse på grunnlag av tidligere opptjening",
        lovreferanse = Paragraf(
            paragraf = "§ 18-2",
            ledd = 6
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