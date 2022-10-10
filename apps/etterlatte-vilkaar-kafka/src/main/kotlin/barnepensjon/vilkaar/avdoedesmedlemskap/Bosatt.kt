package barnepensjon.vilkaar.avdoedesmedlemskap

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.barnepensjon.OpplysningKanIkkeHentesUt
import no.nav.etterlatte.barnepensjon.harKunNorskeAdresserEtterDato
import no.nav.etterlatte.barnepensjon.hentAdresseperioderINorge
import no.nav.etterlatte.barnepensjon.hentGaps
import no.nav.etterlatte.barnepensjon.kombinerPerioder
import no.nav.etterlatte.barnepensjon.opplysningsGrunnlagNull
import no.nav.etterlatte.barnepensjon.setVilkaarVurderingFraKriterier
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.AdresseListe
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.hentBostedsadresse
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentKontaktadresse
import no.nav.etterlatte.libs.common.grunnlag.hentOppholdsadresse
import no.nav.etterlatte.libs.common.grunnlag.hentStatsborgerskap
import no.nav.etterlatte.libs.common.grunnlag.hentUtenlandsopphold
import no.nav.etterlatte.libs.common.grunnlag.hentUtland
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.Metakriterie
import no.nav.etterlatte.libs.common.vikaar.Metakriterietyper
import no.nav.etterlatte.libs.common.vikaar.Utfall
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import java.util.*

fun metakriterieBosattNorge(avdoed: Grunnlagsdata<JsonNode>?): Metakriterie {
    // val ikkeRegistrertIMedl = kriterieIkkeRegistrertIMedl(avdoed) //todo legg til når vi har kobla til register
    val norskStatsborger = kriterieNorskStatsborger(avdoed, Kriterietyper.AVDOED_NORSK_STATSBORGER)
    val ingenInnUtvandring = kriterieIngenInnUtvandring(avdoed, Kriterietyper.AVDOED_INGEN_INN_ELLER_UTVANDRING)
    val ingenUtenlandsoppholdOppgittISoeknad = kriterieIngenUtenlandsoppholdFraSoeknad(
        avdoed,
        Kriterietyper.AVDOED_IKKE_OPPHOLD_UTLAND_FRA_SOEKNAD
    )
    val kunNorskeBostedsadresserSisteFemAar =
        kriterieKunNorskeBostedsadresserSisteFemAar(avdoed, Kriterietyper.AVDOED_KUN_NORSKE_BOSTEDSADRESSER)

    val sammenhengendeBostedsadresserINorgeSisteFemAar =
        kriterieSammenhengendeBostedsadresserINorgeSisteFemAar(
            avdoed,
            Kriterietyper.AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR
        )

    val kunNorskeOppholdsadresserSisteFemAar = kriterieKunNorskeOppholdsadresserSisteFemAar(
        avdoed,
        Kriterietyper.AVDOED_KUN_NORSKE_OPPHOLDSSADRESSER
    )

    val kunNorskeKontaktadresserSisteFemAar = kriterieKunNorskeKontaktadresserSisteFemAar(
        avdoed,
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

    val utfall = when {
        behandlesIPsys.any { it.resultat === VurderingsResultat.IKKE_OPPFYLT } -> Utfall.BEHANDLE_I_PSYS
        trengerAvklaring.any { it.resultat === VurderingsResultat.IKKE_OPPFYLT } -> Utfall.TRENGER_AVKLARING
        else -> Utfall.OPPFYLT
    }

    val kriterieliste = listOf(
        norskStatsborger,
        ingenInnUtvandring,
        ingenUtenlandsoppholdOppgittISoeknad,
        kunNorskeBostedsadresserSisteFemAar,
        sammenhengendeBostedsadresserINorgeSisteFemAar,
        kunNorskeOppholdsadresserSisteFemAar,
        kunNorskeKontaktadresserSisteFemAar
    )

    val resultat = setVilkaarVurderingFraKriterier(kriterieliste)

    return Metakriterie(
        Metakriterietyper.AVDOED_MEDLEMSKAP_BOSTED,
        resultat,
        utfall,
        kriterieliste
    )
}

fun kriterieNorskStatsborger(avdoed: Grunnlagsdata<JsonNode>?, kriterietype: Kriterietyper): Kriterie {
    val statsborgerskap = avdoed?.hentStatsborgerskap()
    val opplysningsGrunnlag = listOfNotNull(
        statsborgerskap?.let {
            Kriteriegrunnlag(
                statsborgerskap.id,
                KriterieOpplysningsType.STATSBORGERSKAP,
                statsborgerskap.kilde,
                statsborgerskap.verdi
            )
        }
    )

    if (avdoed == null) return opplysningsGrunnlagNull(kriterietype, opplysningsGrunnlag)

    val resultat = when (statsborgerskap?.verdi) {
        null -> VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        "NOR" -> VurderingsResultat.OPPFYLT
        else -> VurderingsResultat.IKKE_OPPFYLT
    }

    return Kriterie(kriterietype, resultat, opplysningsGrunnlag)
}

fun kriterieIngenInnUtvandring(avdoed: Grunnlagsdata<JsonNode>?, kriterietype: Kriterietyper): Kriterie {
    val utland = avdoed?.hentUtland()
    val opplysningsGrunnlag = listOfNotNull(
        utland?.let {
            Kriteriegrunnlag(
                it.id,
                KriterieOpplysningsType.UTLAND,
                it.kilde,
                Utland(
                    it.verdi.innflyttingTilNorge,
                    it.verdi.utflyttingFraNorge
                )
            )
        }
    )

    if (avdoed == null) return opplysningsGrunnlagNull(kriterietype, opplysningsGrunnlag)

    val ingenInnUtvandring = utland?.verdi?.innflyttingTilNorge.isNullOrEmpty() &&
        utland?.verdi?.utflyttingFraNorge.isNullOrEmpty()

    return Kriterie(
        kriterietype,
        if (ingenInnUtvandring) VurderingsResultat.OPPFYLT else VurderingsResultat.IKKE_OPPFYLT,
        opplysningsGrunnlag
    )
}

fun kriterieIngenUtenlandsoppholdFraSoeknad(
    avdoed: Grunnlagsdata<JsonNode>?,
    kriterietype: Kriterietyper
): Kriterie {
    val utenlandsopphold = avdoed?.hentUtenlandsopphold()

    return when {
        utenlandsopphold == null -> opplysningsGrunnlagNull(kriterietype, emptyList())
        utenlandsopphold.perioder.isEmpty() -> Kriterie(kriterietype, VurderingsResultat.OPPFYLT, emptyList())
        else -> {
            val seneste = utenlandsopphold.hentSenest()!!
            val opplysningsGrunnlag = listOf(
                Kriteriegrunnlag(
                    seneste.id,
                    KriterieOpplysningsType.AVDOED_UTENLANDSOPPHOLD,
                    seneste.kilde,
                    seneste.verdi
                )
            )

            val ingenOppholdUtlandetFraSoeknad = when (seneste.verdi.harHattUtenlandsopphold) {
                JaNeiVetIkke.NEI -> VurderingsResultat.OPPFYLT
                JaNeiVetIkke.VET_IKKE -> VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
                else -> VurderingsResultat.IKKE_OPPFYLT
            }

            Kriterie(
                kriterietype,
                ingenOppholdUtlandetFraSoeknad,
                opplysningsGrunnlag
            )
        }
    }
}

fun kriterieKunNorskeBostedsadresserSisteFemAar(
    avdoed: Grunnlagsdata<JsonNode>?,
    kriterietype: Kriterietyper
): Kriterie {
    val adresser = avdoed?.hentBostedsadresse()
    val siste = adresser?.hentSenest() ?: return opplysningsGrunnlagNull(kriterietype, emptyList())

    val opplysningsGrunnlag = listOf(
        Kriteriegrunnlag(
            siste.id,
            KriterieOpplysningsType.ADRESSELISTE,
            siste.kilde,
            AdresseListe(adresser.perioder.map { it.verdi })
        )
    )

    return try {
        val femAarFoerDoedsdato = avdoed.hentDoedsdato()?.verdi!!.minusYears(5)
        val vurderingKunNorskeBostedadresser =
            harKunNorskeAdresserEtterDato(adresser.perioder.map { it.verdi }, femAarFoerDoedsdato)

        Kriterie(kriterietype, vurderingKunNorskeBostedadresser, opplysningsGrunnlag)
    } catch (ex: OpplysningKanIkkeHentesUt) {
        Kriterie(kriterietype, VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, opplysningsGrunnlag)
    }
}

fun kriterieKunNorskeOppholdsadresserSisteFemAar(
    avdoed: Grunnlagsdata<JsonNode>?,
    kriterietype: Kriterietyper
): Kriterie {
    val adresser = avdoed?.hentOppholdsadresse() ?: return opplysningsGrunnlagNull(kriterietype, emptyList())

    val opplysningsGrunnlag = listOf(
        Kriteriegrunnlag(
            adresser.id,
            KriterieOpplysningsType.ADRESSELISTE,
            adresser.kilde,
            AdresseListe(adresser.verdi)
        )
    )

    return try {
        val femAarFoerDoedsdato = avdoed.hentDoedsdato()?.verdi!!.minusYears(5)
        val vurderingKunNorskeOppholdsadresser = harKunNorskeAdresserEtterDato(adresser.verdi, femAarFoerDoedsdato)

        Kriterie(kriterietype, vurderingKunNorskeOppholdsadresser, opplysningsGrunnlag)
    } catch (ex: OpplysningKanIkkeHentesUt) {
        Kriterie(kriterietype, VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, opplysningsGrunnlag)
    }
}

fun kriterieKunNorskeKontaktadresserSisteFemAar(
    avdoed: Grunnlagsdata<JsonNode>?,
    kriterietype: Kriterietyper
): Kriterie {
    val adresser = avdoed?.hentKontaktadresse() ?: return opplysningsGrunnlagNull(kriterietype, emptyList())

    val opplysningsGrunnlag = listOf(
        Kriteriegrunnlag(
            adresser.id,
            KriterieOpplysningsType.ADRESSELISTE,
            adresser.kilde,
            AdresseListe(adresser.verdi)
        )
    )

    return try {
        val femAarFoerDoedsdato = avdoed.hentDoedsdato()?.verdi!!.minusYears(5)
        val vurderingKunNorskeKontaktadresser = harKunNorskeAdresserEtterDato(adresser.verdi, femAarFoerDoedsdato)

        Kriterie(kriterietype, vurderingKunNorskeKontaktadresser, opplysningsGrunnlag)
    } catch (ex: OpplysningKanIkkeHentesUt) {
        Kriterie(kriterietype, VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, opplysningsGrunnlag)
    }
}

fun kriterieSammenhengendeBostedsadresserINorgeSisteFemAar(
    avdoed: Grunnlagsdata<JsonNode>?,
    kriterietype: Kriterietyper
): Kriterie {
    val adresser = avdoed?.hentBostedsadresse()
    val doedsdato = avdoed?.hentDoedsdato()
    val foedselsnummer = avdoed?.hentFoedselsnummer()

    val opplysningsGrunnlag = listOfNotNull(
        adresser?.hentSenest()?.let {
            Kriteriegrunnlag(
                it.id,
                KriterieOpplysningsType.ADRESSELISTE,
                it.kilde,
                AdresseListe(adresser.perioder.map { it.verdi })
            )
        },
        doedsdato?.let {
            Kriteriegrunnlag(
                it.id,
                KriterieOpplysningsType.DOEDSDATO,
                it.kilde,
                Doedsdato(it.verdi, foedselsnummer!!.verdi)
            )
        }
    )

    if (adresser == null || doedsdato?.verdi == null) {
        return opplysningsGrunnlagNull(kriterietype, opplysningsGrunnlag)
    }

    try {
        val femAarFoerDoedsdato = doedsdato.verdi!!.minusYears(5)

        val bostedperiode = hentAdresseperioderINorge(adresser.perioder.map { it.verdi }, doedsdato.verdi!!)
        val kombinerteBostedsperioder = kombinerPerioder(bostedperiode)
        val periodeGaps = hentGaps(kombinerteBostedsperioder, femAarFoerDoedsdato, doedsdato.verdi!!)

        val vurderingBoddSammenhengendeINorge = if (periodeGaps.isEmpty()) {
            VurderingsResultat.OPPFYLT
        } else {
            VurderingsResultat.IKKE_OPPFYLT
        }

        val oppdatertGrunnlag = if (periodeGaps.isNotEmpty()) {
            val gapGrunnlag = Kriteriegrunnlag(
                UUID.randomUUID(),
                KriterieOpplysningsType.ADRESSE_GAPS,
                Grunnlagsopplysning.Vilkaarskomponenten("vilkaarskomponenten"),
                periodeGaps
            )
            opplysningsGrunnlag + gapGrunnlag
        } else {
            opplysningsGrunnlag
        }

        return Kriterie(kriterietype, vurderingBoddSammenhengendeINorge, oppdatertGrunnlag)
    } catch (ex: OpplysningKanIkkeHentesUt) {
        return Kriterie(kriterietype, VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, opplysningsGrunnlag)
    }
}