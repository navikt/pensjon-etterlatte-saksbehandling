package no.nav.etterlatte.grunnlagsendring

import no.nav.etterlatte.behandling.domain.SamsvarMellomKildeOgGrunnlag
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Sivilstand
import no.nav.etterlatte.libs.common.person.Utland
import java.time.LocalDate

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
