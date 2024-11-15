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
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstand
import no.nav.etterlatte.libs.common.person.Utland
import java.time.LocalDate

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
            SamsvarMellomKildeOgGrunnlag.Folkeregisteridentifikatorsamsvar(false) // TODO("Må finne ut av denne")
        }

        GrunnlagsendringsType.BOSTED -> {
            val pdlBosted = pdlData.hentBostedsadresse()
            val grunnlagBosted = grunnlag?.bostedsadresse(rolle, fnr)?.verdi
            samsvarBostedsadresse(pdlBosted, grunnlagBosted)
        }

        GrunnlagsendringsType.GRUNNBELOEP -> {
            SamsvarMellomKildeOgGrunnlag.Grunnbeloep(samsvar = false)
        }

        GrunnlagsendringsType.INSTITUSJONSOPPHOLD, GrunnlagsendringsType.UFOERETRYGD -> {
            throw IllegalStateException("Denne hendelsen skal gå rett til oppgavelisten og aldri komme hit")
        }
    }
}

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
) = SamsvarMellomKildeOgGrunnlag.Adresse(
    fraPdl = adressePdl,
    fraGrunnlag = adresseGrunnlag,
    samsvar = adressePdl erLikRekkefoelgeIgnorert adresseGrunnlag,
)

infix fun <T> List<T>?.erLikRekkefoelgeIgnorert(other: List<T>?): Boolean = this?.size == other?.size && this?.toSet() == other?.toSet()
