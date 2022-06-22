package no.nav.etterlatte.barnepensjon

import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Adresser
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.inntekt.Inntekt
import no.nav.etterlatte.libs.common.inntekt.PensjonUforeOpplysning
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import java.time.LocalDateTime
import java.util.*


fun vilkaarAvdoedesMedlemskap(
    vilkaartype: Vilkaartyper,
    avdoedSoeknad: VilkaarOpplysning<AvdoedSoeknad>?,
    avdoedPdl: VilkaarOpplysning<Person>?,
    pensjonUforeOpplysning: VilkaarOpplysning<PensjonUforeOpplysning>?
): VurdertVilkaar {
    // Kriterier: 1. bodd i norge siste 5 årene
    // 2. Arbeidet i norge siste 5 årene
    // 3. ingen opphold utenfor Norge
    // ELLER :
    // 4. mottatt trydg / uføre eller pensjon siste 5 årene

    val utenlandsoppholdOppgittISoeknad = kriterieIngenUtenlandsoppholdFraSoeknadSisteFemAar(
        avdoedSoeknad,
        avdoedPdl,
        Kriterietyper.AVDOED_IKKE_OPPHOLD_UTLAND_FRA_SOEKNAD
    )

    val utenlandsoppholdSisteFemAarPdl =
        kriterieSammenhengendeAdresserINorgeSisteFemAar(
            avdoedPdl,
            Kriterietyper.AVDOED_SAMMENHENGENDE_ADRESSE_NORGE_SISTE_FEM_AAR
        )

    val harMottattUforeTrygdSisteFemAar =
        kriterieHarMottattUforeTrygdSisteFemAar(pensjonUforeOpplysning?.opplysning?.mottattUforetrygd)
    val harMottattPensjonSisteFemAar =
        kriterieHarMottattPensjonSisteFemAar(pensjonUforeOpplysning?.opplysning?.mottattAlderspensjon)

    val harHatt100prosentStillingSisteFemAar = false //todo

    return VurdertVilkaar(
        vilkaartype,
        VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, //endre når vi får inn flere opplysninger
        listOf(utenlandsoppholdOppgittISoeknad, utenlandsoppholdSisteFemAarPdl, harMottattUforeTrygdSisteFemAar, harMottattPensjonSisteFemAar),
        LocalDateTime.now()
    )
}

fun kriterieSammenhengendeAdresserINorgeSisteFemAar(
    avdoedPdl: VilkaarOpplysning<Person>?,
    kriterietype: Kriterietyper
): Kriterie {
    val opplysningsGrunnlag = listOfNotNull(
        avdoedPdl?.let {
            Kriteriegrunnlag(
                avdoedPdl.id,
                KriterieOpplysningsType.ADRESSER,
                avdoedPdl.kilde,
                Adresser(it.opplysning.bostedsadresse, it.opplysning.oppholdsadresse, it.opplysning.kontaktadresse)
            )
        },
        avdoedPdl?.let {
            Kriteriegrunnlag(
                avdoedPdl.id,
                KriterieOpplysningsType.DOEDSDATO,
                avdoedPdl.kilde,
                Doedsdato(avdoedPdl.opplysning.doedsdato, avdoedPdl.opplysning.foedselsnummer)
            )
        },
    )

    if (avdoedPdl == null) return opplysningsGrunnlagNull(
        Kriterietyper.AVDOED_SAMMENHENGENDE_ADRESSE_NORGE_SISTE_FEM_AAR,
        opplysningsGrunnlag
    )

    try {
        val adresser = hentAdresser(avdoedPdl)
        val doedsdato = hentDoedsdato(avdoedPdl)
        val femAarFoerDoedsdato = hentDoedsdato(avdoedPdl).minusYears(5)

        val bostedperiode = hentAdresseperioderINorge(adresser.bostedadresse, doedsdato)
        val oppholdperiode = hentAdresseperioderINorge(adresser.oppholdadresse, doedsdato)
        //hva skal brukes som grunnlag?
        val kombinertliste = listOf(bostedperiode).filterNotNull().flatten()

        val kombinerteperioder = kombinerPerioder(kombinertliste)
        val periodeGaps = hentGaps(kombinerteperioder, femAarFoerDoedsdato, doedsdato)

        val vurderingBoddSammenhengendeINorge = if (periodeGaps.isEmpty()) {
            VurderingsResultat.OPPFYLT
        } else {
            VurderingsResultat.IKKE_OPPFYLT
        }
        val vurderingKunNorskeAdresserPdl = harKunNorskePdlAdresserEtterDato(adresser, femAarFoerDoedsdato)

        // wrappe i if om gaps finnes?
        val oppdatertGrunnlag = opplysningsGrunnlag + Kriteriegrunnlag(
            UUID.randomUUID(), KriterieOpplysningsType.ADRESSE_GAPS,
            Grunnlagsopplysning.Vilkaarskomponenten("vilkaarskomponenten"),
            periodeGaps
        )

        val resultat = hentVurdering(listOf(vurderingKunNorskeAdresserPdl, vurderingBoddSammenhengendeINorge))

        return Kriterie(kriterietype, resultat, oppdatertGrunnlag)
    } catch (ex: OpplysningKanIkkeHentesUt) {
        return Kriterie(kriterietype, VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, opplysningsGrunnlag)
    }
}

fun kriterieIngenUtenlandsoppholdFraSoeknadSisteFemAar(
    avdoedSoeknad: VilkaarOpplysning<AvdoedSoeknad>?,
    avdoedPdl: VilkaarOpplysning<Person>?,
    kriterietype: Kriterietyper
): Kriterie {

    val opplysningsGrunnlag = listOfNotNull(
        avdoedPdl?.let {
            Kriteriegrunnlag(
                avdoedPdl.id,
                KriterieOpplysningsType.DOEDSDATO,
                avdoedPdl.kilde,
                Doedsdato(avdoedPdl.opplysning.doedsdato, avdoedPdl.opplysning.foedselsnummer)
            )
        },
        avdoedSoeknad?.let {
            Kriteriegrunnlag(
                avdoedSoeknad.id,
                KriterieOpplysningsType.AVDOED_UTENLANDSOPPHOLD,
                avdoedSoeknad.kilde,
                avdoedSoeknad.opplysning.utenlandsopphold
            )
        }
    )

    if (avdoedPdl == null || avdoedSoeknad == null) return opplysningsGrunnlagNull(
        kriterietype,
        opplysningsGrunnlag
    )

    val ingenOppholdUtlandetFraSoeknad = try {
        val utenlandsoppholdSoeknad = avdoedSoeknad.opplysning.utenlandsopphold
        if (utenlandsoppholdSoeknad.harHattUtenlandsopphold === JaNeiVetIkke.NEI) {
            VurderingsResultat.OPPFYLT
        } else if (utenlandsoppholdSoeknad.harHattUtenlandsopphold === JaNeiVetIkke.VET_IKKE) {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        } else {
            val femAarFoerDoedsdato = hentDoedsdato(avdoedPdl).minusYears(5)

            val tilDatoer = utenlandsoppholdSoeknad.opphold?.map { it.tilDato }
            val oppholdSisteFemAAr = tilDatoer?.map { it?.isAfter(femAarFoerDoedsdato) }
            val tilDatoNull = tilDatoer?.map { it == null }

            if (tilDatoNull != null && tilDatoNull.contains(true)) {
                VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
            } else if (oppholdSisteFemAAr != null && oppholdSisteFemAAr.contains(true)) {
                VurderingsResultat.IKKE_OPPFYLT
            } else {
                VurderingsResultat.OPPFYLT
            }
        }

    } catch (ex: OpplysningKanIkkeHentesUt) {
        VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    }

    return Kriterie(
        kriterietype, ingenOppholdUtlandetFraSoeknad, opplysningsGrunnlag
    )
}

fun kriterieHarMottattPensjonSisteFemAar(harMottattPensjonSisteFemAar: List<Inntekt>?): Kriterie {
    val opplysningsGrunnlag = listOfNotNull(
        harMottattPensjonSisteFemAar?.let {
            Kriteriegrunnlag(
                UUID.randomUUID(), //todo: må settes i grunnlag
                KriterieOpplysningsType.AVDOED_UFORE_PENSJON,
                Grunnlagsopplysning.Inntektskomponenten("inntektskomponenten"),
                harMottattPensjonSisteFemAar
            )
        }
    )

    if (harMottattPensjonSisteFemAar == null) return opplysningsGrunnlagNull(
        Kriterietyper.AVDOED_HAR_MOTTATT_PENSJON_SISTE_FEM_AAR,
        opplysningsGrunnlag
    )

    val resultat = VurderingsResultat.OPPFYLT //TODO logikk her

    return Kriterie(
        Kriterietyper.AVDOED_HAR_MOTTATT_PENSJON_SISTE_FEM_AAR,
        resultat,
        opplysningsGrunnlag
    )
}


fun kriterieHarMottattUforeTrygdSisteFemAar(harMottattUforeTrygdSisteFemAar: List<Inntekt>?): Kriterie {
    val opplysningsGrunnlag = listOfNotNull(
        harMottattUforeTrygdSisteFemAar?.let {
            Kriteriegrunnlag(
                UUID.randomUUID(), //todo: må settes i grunnlag
                KriterieOpplysningsType.AVDOED_UFORE_PENSJON,
                Grunnlagsopplysning.Inntektskomponenten("inntektskomponenten"),
                harMottattUforeTrygdSisteFemAar
            )
        }
    )

    if (harMottattUforeTrygdSisteFemAar == null) return opplysningsGrunnlagNull(
        Kriterietyper.AVDOED_HAR_MOTTATT_TRYGD_SISTE_FEM_AAR,
        opplysningsGrunnlag
    )

    val resultat = VurderingsResultat.OPPFYLT //TODO logikk her

    return Kriterie(
        Kriterietyper.AVDOED_HAR_MOTTATT_TRYGD_SISTE_FEM_AAR,
        resultat,
        opplysningsGrunnlag
    )
}