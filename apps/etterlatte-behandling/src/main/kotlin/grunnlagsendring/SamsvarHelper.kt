package no.nav.etterlatte.grunnlagsendring

import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.behandling.domain.SamsvarMellomKildeOgGrunnlag
import no.nav.etterlatte.common.klienter.hentAnsvarligeForeldre
import no.nav.etterlatte.common.klienter.hentBarn
import no.nav.etterlatte.common.klienter.hentBostedsadresse
import no.nav.etterlatte.common.klienter.hentDoedsdato
import no.nav.etterlatte.common.klienter.hentSivilstand
import no.nav.etterlatte.common.klienter.hentUtland
import no.nav.etterlatte.common.klienter.hentVergemaal
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstand
import no.nav.etterlatte.libs.common.person.Utland
import java.time.LocalDate
import java.time.LocalDateTime

internal fun finnSamsvarForHendelse(
    hendelse: Grunnlagsendringshendelse,
    pdlData: PersonDTO,
    grunnlag: Grunnlag?,
    personRolle: PersonRolle,
    sakType: SakType,
): SamsvarMellomKildeOgGrunnlag {
    val rolle = hendelse.hendelseGjelderRolle
    val fnr = hendelse.gjelderPerson

    return when (hendelse.type) {
        GrunnlagsendringsType.DOEDSFALL -> {
            samsvarDoedsdatoer(
                doedsdatoPdl = pdlData.hentDoedsdato(),
                doedsdatoGrunnlag = grunnlag?.doedsdato(rolle, fnr)?.verdi,
            )
        }

        GrunnlagsendringsType.UTFLYTTING -> {
            samsvarUtflytting(
                utflyttingPdl = pdlData.hentUtland(),
                utflyttingGrunnlag = grunnlag?.utland(rolle, fnr),
            )
        }

        GrunnlagsendringsType.FORELDER_BARN_RELASJON -> {
            if (personRolle in listOf(PersonRolle.BARN, PersonRolle.TILKNYTTET_BARN)) {
                samsvarAnsvarligeForeldre(
                    ansvarligeForeldrePdl = pdlData.hentAnsvarligeForeldre(),
                    ansvarligeForeldreGrunnlag = grunnlag?.ansvarligeForeldre(rolle, fnr),
                )
            } else {
                samsvarBarn(
                    barnPdl = pdlData.hentBarn(),
                    barnGrunnlag = grunnlag?.barn(rolle),
                )
            }
        }

        GrunnlagsendringsType.VERGEMAAL_ELLER_FREMTIDSFULLMAKT -> {
            val pdlVergemaal = pdlData.hentVergemaal()
            val grunnlagVergemaal = grunnlag?.vergemaalellerfremtidsfullmakt(rolle)
            SamsvarMellomKildeOgGrunnlag.VergemaalEllerFremtidsfullmaktForhold(
                fraPdl = pdlVergemaal,
                fraGrunnlag = grunnlagVergemaal,
                samsvar = pdlVergemaal erLikRekkefoelgeIgnorert grunnlagVergemaal,
            )
        }

        GrunnlagsendringsType.SIVILSTAND -> {
            when (sakType) {
                SakType.BARNEPENSJON -> samsvarSivilstandBP()
                SakType.OMSTILLINGSSTOENAD -> {
                    val pdlSivilstand = pdlData.hentSivilstand()
                    val grunnlagSivilstand = grunnlag?.sivilstand(rolle)
                    samsvarSivilstandOMS(pdlSivilstand, grunnlagSivilstand)
                }
            }
        }

        GrunnlagsendringsType.FOLKEREGISTERIDENTIFIKATOR -> {
            samsvarFolkeregisterIdent(
                identPdl = pdlData.foedselsnummer.verdi,
                identGrunnlag = grunnlag?.soeker?.hentFoedselsnummer()?.verdi,
            )
        }

        GrunnlagsendringsType.BOSTED -> {
            // Ikke interessant med hendelser på søsken
            if (personRolle == PersonRolle.TILKNYTTET_BARN && sakType == SakType.BARNEPENSJON) {
                return SamsvarMellomKildeOgGrunnlag.Adresse(
                    samsvar = true,
                    fraPdl = null,
                    aarsakIgnorert = "HENDELSE_SOESKEN",
                    fraGrunnlag = null,
                )
            }
            if (personRolle == PersonRolle.TILKNYTTET_BARN && sakType == SakType.OMSTILLINGSSTOENAD) {
                val foedselsdato = pdlData.foedselsdato?.verdi
                val erOver18Aar = foedselsdato?.plusYears(18)?.plusDays(1)?.isAfter(LocalDate.now())
                if (erOver18Aar == true) {
                    return SamsvarMellomKildeOgGrunnlag.Adresse(
                        samsvar = true,
                        fraPdl = null,
                        aarsakIgnorert = "BARN_OVER_18AAR",
                        fraGrunnlag = null,
                    )
                }
            }

            val pdlBosted = pdlData.hentBostedsadresse()
            val grunnlagBosted = grunnlag?.bostedsadresse(rolle, fnr)?.verdi
            samsvarBostedsadresse(pdlBosted, grunnlagBosted)
        }

        GrunnlagsendringsType.INSTITUSJONSOPPHOLD, GrunnlagsendringsType.UFOERETRYGD -> {
            throw IllegalStateException("Denne hendelsen skal gå rett til oppgavelisten og aldri komme hit")
        }
    }
}

fun samsvarFolkeregisterIdent(
    identPdl: Folkeregisteridentifikator?,
    identGrunnlag: Folkeregisteridentifikator?,
) = SamsvarMellomKildeOgGrunnlag.Folkeregisteridentifikatorsamsvar(
    fraPdl = identPdl,
    fraGrunnlag = identGrunnlag,
    samsvar = identPdl == identGrunnlag,
)

fun samsvarDoedsdatoer(
    doedsdatoPdl: LocalDate?,
    doedsdatoGrunnlag: LocalDate?,
) = SamsvarMellomKildeOgGrunnlag.Doedsdatoforhold(
    fraGrunnlag = doedsdatoGrunnlag,
    fraPdl = doedsdatoPdl,
    samsvar = doedsdatoPdl == doedsdatoGrunnlag,
)

fun samsvarAnsvarligeForeldre(
    ansvarligeForeldrePdl: List<Folkeregisteridentifikator>?,
    ansvarligeForeldreGrunnlag: List<Folkeregisteridentifikator>?,
) = SamsvarMellomKildeOgGrunnlag.AnsvarligeForeldre(
    fraPdl = ansvarligeForeldrePdl,
    fraGrunnlag = ansvarligeForeldreGrunnlag,
    samsvar = ansvarligeForeldrePdl erLikRekkefoelgeIgnorert ansvarligeForeldreGrunnlag,
)

fun samsvarBarn(
    barnPdl: List<Folkeregisteridentifikator>?,
    barnGrunnlag: List<Folkeregisteridentifikator>?,
) = SamsvarMellomKildeOgGrunnlag.Barn(
    fraPdl = barnPdl,
    fraGrunnlag = barnGrunnlag,
    samsvar = barnPdl erLikRekkefoelgeIgnorert barnGrunnlag,
)

fun samsvarUtflytting(
    utflyttingPdl: Utland?,
    utflyttingGrunnlag: Utland?,
) = SamsvarMellomKildeOgGrunnlag.Utlandsforhold(
    fraPdl = utflyttingPdl,
    fraGrunnlag = utflyttingGrunnlag,
    samsvar =
        utflyttingPdl?.utflyttingFraNorge erLikRekkefoelgeIgnorert utflyttingGrunnlag?.utflyttingFraNorge &&
            utflyttingPdl?.innflyttingTilNorge erLikRekkefoelgeIgnorert utflyttingGrunnlag?.innflyttingTilNorge,
)

fun samsvarSivilstandOMS(
    sivilstandPdl: List<Sivilstand>?,
    sivilstandGrunnlag: List<Sivilstand>?,
) = SamsvarMellomKildeOgGrunnlag.Sivilstand(
    fraPdl = sivilstandPdl,
    fraGrunnlag = sivilstandGrunnlag,
    samsvar = sivilstandPdl erLikRekkefoelgeIgnorert sivilstandGrunnlag,
)

// Sivilstandhendelser er ikke relevant for BP
fun samsvarSivilstandBP() =
    SamsvarMellomKildeOgGrunnlag.Sivilstand(
        fraPdl = null,
        fraGrunnlag = null,
        samsvar = true,
    )

fun samsvarBostedsadresse(
    adressePdl: List<Adresse>?,
    adresseGrunnlag: List<Adresse>?,
): SamsvarMellomKildeOgGrunnlag.Adresse {
    val naavaerendeAdresseLik = erNaavaerendeAdresseLik(adressePdl ?: emptyList(), adresseGrunnlag ?: emptyList())
    val alleAdresserLike = adressePdl erLikRekkefoelgeIgnorert adresseGrunnlag

    return SamsvarMellomKildeOgGrunnlag.Adresse(
        fraPdl = adressePdl,
        fraGrunnlag = adresseGrunnlag,
        samsvar = naavaerendeAdresseLik,
        aarsakIgnorert = "FORSKJELL_KUN_HISTORISK".takeIf { naavaerendeAdresseLik && !alleAdresserLike },
    )
}

fun erNaavaerendeAdresseLik(
    adressePdl: List<Adresse>,
    adresseGrunnlag: List<Adresse>,
): Boolean {
    val naavaerendePdl = adressePdl.naavaerende()
    val naavaerendGrunnlag = adresseGrunnlag.naavaerende()
    return naavaerendGrunnlag == naavaerendePdl
}

fun List<Adresse>.naavaerende(): Adresse? =
    this.filter { it.aktiv }.maxByOrNull {
        it.gyldigFraOgMed ?: LocalDateTime.of(1900, 1, 1, 0, 0)
    } ?: this.maxByOrNull { it.gyldigFraOgMed ?: LocalDateTime.of(1900, 1, 1, 0, 0) }

infix fun <T> List<T>?.erLikRekkefoelgeIgnorert(other: List<T>?): Boolean = this?.size == other?.size && this?.toSet() == other?.toSet()
