package barnepensjon.vilkaar.avdoedesmedlemskap

import no.nav.etterlatte.barnepensjon.OpplysningKanIkkeHentesUt
import no.nav.etterlatte.barnepensjon.harKunNorskeAdresserEtterDato
import no.nav.etterlatte.barnepensjon.hentAdresseperioderINorge
import no.nav.etterlatte.barnepensjon.hentAdresser
import no.nav.etterlatte.barnepensjon.hentBostedsAdresser
import no.nav.etterlatte.barnepensjon.hentDoedsdato
import no.nav.etterlatte.barnepensjon.hentGaps
import no.nav.etterlatte.barnepensjon.hentKontaktAdresser
import no.nav.etterlatte.barnepensjon.hentOppholdsAdresser
import no.nav.etterlatte.barnepensjon.kombinerPerioder
import no.nav.etterlatte.barnepensjon.opplysningsGrunnlagNull
import no.nav.etterlatte.barnepensjon.setVikaarVurderingFraKriterier
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Adresser
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Bostedadresser
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.Metakriterie
import no.nav.etterlatte.libs.common.vikaar.Metakriterietyper
import no.nav.etterlatte.libs.common.vikaar.Utfall
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import java.util.*

fun metakriterieBosattNorge(
    avdoedSoeknad: VilkaarOpplysning<AvdoedSoeknad>?,
    avdoedPdl: VilkaarOpplysning<Person>?,
): Metakriterie {
    //val ikkeRegistrertIMedl = kriterieIkkeRegistrertIMedl(avdoedPdl) //todo legg til når vi har kobla til register
    val norskStatsborger = kriterieNorskStatsborger(avdoedPdl, Kriterietyper.AVDOED_NORSK_STATSBORGER)
    val ingenInnUtvandring = kriterieIngenInnUtvandring(avdoedPdl, Kriterietyper.AVDOED_INGEN_INN_ELLER_UTVANDRING)
    val ingenUtenlandsoppholdOppgittISoeknad = kriterieIngenUtenlandsoppholdFraSoeknad(
        avdoedSoeknad,
        Kriterietyper.AVDOED_IKKE_OPPHOLD_UTLAND_FRA_SOEKNAD
    )
    val kunNorskeBostedsadresserSisteFemAar =
        kriterieKunNorskeBostedsadresserSisteFemAar(avdoedPdl, Kriterietyper.AVDOED_KUN_NORSKE_BOSTEDSADRESSER)

    val sammenhengendeBostedsadresserINorgeSisteFemAar =
        kriterieSammenhengendeAdresserINorgeSisteFemAar(
            avdoedPdl,
            Kriterietyper.AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR
        )

    val kunNorskeOppholdsadresserSisteFemAar = kriterieKunNorskeOppholdsadresserSisteFemAar(
        avdoedPdl,
        Kriterietyper.AVDOED_KUN_NORSKE_OPPHOLDSSADRESSER
    )

    val kunNorskeKontaktadresserSisteFemAar = kriterieKunNorskeKontaktadresserSisteFemAar(
        avdoedPdl,
        Kriterietyper.AVDOED_KUN_NORSKE_KONTAKTADRESSER
    )

    val behandlesIPsys = listOf(
        norskStatsborger,
        ingenInnUtvandring,
        ingenUtenlandsoppholdOppgittISoeknad,
        kunNorskeBostedsadresserSisteFemAar
    )

    val trengerAvklaring = listOf(
        sammenhengendeBostedsadresserINorgeSisteFemAar,
        kunNorskeOppholdsadresserSisteFemAar,
        kunNorskeKontaktadresserSisteFemAar
    )

    val utfallPsys = behandlesIPsys.any { it.resultat === VurderingsResultat.IKKE_OPPFYLT }
    val utfallTrengerAvklaring = trengerAvklaring.any { it.resultat === VurderingsResultat.IKKE_OPPFYLT }
    val utfall = if (utfallPsys) {
        Utfall.BEHANDLE_I_PSYS
    } else if (utfallTrengerAvklaring) {
        Utfall.TRENGER_AVKLARING
    } else {
        Utfall.OPPFYLT
    }

    val kriterieliste = listOf<Kriterie>(
        norskStatsborger,
        ingenInnUtvandring,
        ingenUtenlandsoppholdOppgittISoeknad,
        kunNorskeBostedsadresserSisteFemAar,
        sammenhengendeBostedsadresserINorgeSisteFemAar,
        kunNorskeOppholdsadresserSisteFemAar,
        kunNorskeKontaktadresserSisteFemAar,
    )

    val resultat = setVikaarVurderingFraKriterier(kriterieliste)

    return Metakriterie(
        Metakriterietyper.AVDOED_MEDLEMSKAP_BOSTED,
        resultat,
        utfall,
        kriterieliste
    )

}

fun kriterieNorskStatsborger(avdoedPdl: VilkaarOpplysning<Person>?, kriterietype: Kriterietyper): Kriterie {
    val NORGE = "NOR"
    val norskStatsborger = avdoedPdl?.opplysning?.statsborgerskap === NORGE

    val opplysningsGrunnlag = listOfNotNull(
        avdoedPdl?.let {
            Kriteriegrunnlag(
                avdoedPdl.id,
                KriterieOpplysningsType.STATSBORGERSKAP,
                avdoedPdl.kilde,
                avdoedPdl.opplysning.statsborgerskap.toString(),
            )
        })

    if (avdoedPdl == null) return opplysningsGrunnlagNull(kriterietype, opplysningsGrunnlag)
    val resultat = if (avdoedPdl.opplysning.statsborgerskap == null) {
        VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    } else if (norskStatsborger) {
        VurderingsResultat.OPPFYLT
    } else VurderingsResultat.IKKE_OPPFYLT

    return Kriterie(kriterietype, resultat, opplysningsGrunnlag)
}

fun kriterieIngenInnUtvandring(avdoedPdl: VilkaarOpplysning<Person>?, kriterietype: Kriterietyper): Kriterie {
    val ingenInnUtvandring = avdoedPdl?.opplysning?.utland?.innflyttingTilNorge == null
            && avdoedPdl?.opplysning?.utland?.utflyttingFraNorge == null

    val opplysningsGrunnlag = listOfNotNull(
        avdoedPdl?.let {
            Kriteriegrunnlag(
                avdoedPdl.id,
                KriterieOpplysningsType.UTLAND,
                avdoedPdl.kilde,
                Utland(
                    avdoedPdl.opplysning.utland?.innflyttingTilNorge,
                    avdoedPdl.opplysning.utland?.utflyttingFraNorge
                ),
            )
        }
    )

    if (avdoedPdl == null) return opplysningsGrunnlagNull(kriterietype, opplysningsGrunnlag)

    return Kriterie(
        kriterietype,
        if (ingenInnUtvandring) VurderingsResultat.OPPFYLT else VurderingsResultat.IKKE_OPPFYLT,
        opplysningsGrunnlag
    )

}

fun kriterieIngenUtenlandsoppholdFraSoeknad(
    avdoedSoeknad: VilkaarOpplysning<AvdoedSoeknad>?,
    kriterietype: Kriterietyper
): Kriterie {

    val opplysningsGrunnlag = listOfNotNull(
        avdoedSoeknad?.let {
            Kriteriegrunnlag(
                avdoedSoeknad.id,
                KriterieOpplysningsType.AVDOED_UTENLANDSOPPHOLD,
                avdoedSoeknad.kilde,
                avdoedSoeknad.opplysning.utenlandsopphold
            )
        }
    )

    if (avdoedSoeknad == null) return opplysningsGrunnlagNull(kriterietype, opplysningsGrunnlag)

    val utenlandsoppholdSoeknad = avdoedSoeknad.opplysning.utenlandsopphold

    val ingenOppholdUtlandetFraSoeknad =
        if (utenlandsoppholdSoeknad.harHattUtenlandsopphold === JaNeiVetIkke.NEI) {
            VurderingsResultat.OPPFYLT
        } else if (utenlandsoppholdSoeknad.harHattUtenlandsopphold === JaNeiVetIkke.VET_IKKE) {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        } else {
            VurderingsResultat.IKKE_OPPFYLT
        }

    return Kriterie(
        kriterietype, ingenOppholdUtlandetFraSoeknad, opplysningsGrunnlag
    )
}

fun kriterieKunNorskeBostedsadresserSisteFemAar(
    avdoedPdl: VilkaarOpplysning<Person>?,
    kriterietype: Kriterietyper
): Kriterie {
    val opplysningsGrunnlag = listOfNotNull(
        avdoedPdl?.let {
            Kriteriegrunnlag(
                avdoedPdl.id,
                KriterieOpplysningsType.ADRESSER,
                avdoedPdl.kilde,
                Bostedadresser(it.opplysning.bostedsadresse)
            )
        })

    if (avdoedPdl == null) return opplysningsGrunnlagNull(kriterietype, opplysningsGrunnlag)

    try {
        val adresser = hentBostedsAdresser(avdoedPdl)
        val femAarFoerDoedsdato = hentDoedsdato(avdoedPdl).minusYears(5)
        val vurderingKunNorskeBostedadresser = harKunNorskeAdresserEtterDato(adresser, femAarFoerDoedsdato)

        return Kriterie(kriterietype, vurderingKunNorskeBostedadresser, opplysningsGrunnlag)
    } catch (ex: OpplysningKanIkkeHentesUt) {
        return Kriterie(kriterietype, VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, opplysningsGrunnlag)
    }
}

fun kriterieKunNorskeOppholdsadresserSisteFemAar(
    avdoedPdl: VilkaarOpplysning<Person>?,
    kriterietype: Kriterietyper
): Kriterie {
    val opplysningsGrunnlag = listOfNotNull(
        avdoedPdl?.let {
            Kriteriegrunnlag(
                avdoedPdl.id,
                KriterieOpplysningsType.ADRESSER,
                avdoedPdl.kilde,
                listOf(it.opplysning.oppholdsadresse)
            )
        })

    if (avdoedPdl == null) return opplysningsGrunnlagNull(kriterietype, opplysningsGrunnlag)

    try {
        val adresser = hentOppholdsAdresser(avdoedPdl)
        val femAarFoerDoedsdato = hentDoedsdato(avdoedPdl).minusYears(5)
        val vurderingKunNorskeOppholdsadresser = harKunNorskeAdresserEtterDato(adresser, femAarFoerDoedsdato)

        return Kriterie(kriterietype, vurderingKunNorskeOppholdsadresser, opplysningsGrunnlag)
    } catch (ex: OpplysningKanIkkeHentesUt) {
        return Kriterie(kriterietype, VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, opplysningsGrunnlag)
    }
}

fun kriterieKunNorskeKontaktadresserSisteFemAar(
    avdoedPdl: VilkaarOpplysning<Person>?,
    kriterietype: Kriterietyper
): Kriterie {
    val opplysningsGrunnlag = listOfNotNull(
        avdoedPdl?.let {
            Kriteriegrunnlag(
                avdoedPdl.id,
                KriterieOpplysningsType.ADRESSER,
                avdoedPdl.kilde,
                listOf(it.opplysning.kontaktadresse)
            )
        })

    if (avdoedPdl == null) return opplysningsGrunnlagNull(kriterietype, opplysningsGrunnlag)

    try {
        val adresser = hentKontaktAdresser(avdoedPdl)
        val femAarFoerDoedsdato = hentDoedsdato(avdoedPdl).minusYears(5)
        val vurderingKunNorskeKontaktadresser = harKunNorskeAdresserEtterDato(adresser, femAarFoerDoedsdato)

        return Kriterie(kriterietype, vurderingKunNorskeKontaktadresser, opplysningsGrunnlag)
    } catch (ex: OpplysningKanIkkeHentesUt) {
        return Kriterie(kriterietype, VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, opplysningsGrunnlag)
    }
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

    if (avdoedPdl == null) return opplysningsGrunnlagNull(kriterietype, opplysningsGrunnlag)

    try {
        val adresser = hentAdresser(avdoedPdl)
        val doedsdato = hentDoedsdato(avdoedPdl)
        val femAarFoerDoedsdato = hentDoedsdato(avdoedPdl).minusYears(5)

        val bostedperiode = hentAdresseperioderINorge(adresser.bostedadresse, doedsdato)
        val kombinerteBostedsperioder = kombinerPerioder(bostedperiode)
        val periodeGaps = hentGaps(kombinerteBostedsperioder, femAarFoerDoedsdato, doedsdato)

        val vurderingBoddSammenhengendeINorge = if (periodeGaps.isEmpty()) {
            VurderingsResultat.OPPFYLT
        } else {
            VurderingsResultat.IKKE_OPPFYLT
        }

        val gapGrunnlag = Kriteriegrunnlag(
            UUID.randomUUID(), KriterieOpplysningsType.ADRESSE_GAPS,
            Grunnlagsopplysning.Vilkaarskomponenten("vilkaarskomponenten"),
            periodeGaps
        )

        val oppdatertGrunnlag = if (periodeGaps.isNotEmpty()) {
            opplysningsGrunnlag + gapGrunnlag
        } else {
            opplysningsGrunnlag
        }

        return Kriterie(kriterietype, vurderingBoddSammenhengendeINorge, oppdatertGrunnlag)
    } catch (ex: OpplysningKanIkkeHentesUt) {
        return Kriterie(kriterietype, VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, opplysningsGrunnlag)
    }
}