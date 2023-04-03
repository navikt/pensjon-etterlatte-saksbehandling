package no.nav.etterlatte.grunnlagsendring

import no.nav.etterlatte.behandling.domain.SamsvarMellomPdlOgGrunnlag
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Utland
import java.time.LocalDate

fun samsvarDoedsdatoer(
    doedsdatoPdl: LocalDate?,
    doedsdatoGrunnlag: LocalDate?
) = SamsvarMellomPdlOgGrunnlag.Doedsdatoforhold(
    fraGrunnlag = doedsdatoGrunnlag,
    fraPdl = doedsdatoPdl,
    samsvar = doedsdatoPdl == doedsdatoGrunnlag
)

fun samsvarAnsvarligeForeldre(
    ansvarligeForeldrePdl: List<Folkeregisteridentifikator>?,
    ansvarligeForeldreGrunnlag: List<Folkeregisteridentifikator>?
) = SamsvarMellomPdlOgGrunnlag.AnsvarligeForeldre(
    fraPdl = ansvarligeForeldrePdl,
    fraGrunnlag = ansvarligeForeldreGrunnlag,
    samsvar = ansvarligeForeldrePdl erLikRekkefoelgeIgnorert ansvarligeForeldreGrunnlag
)

fun samsvarBarn(
    barnPdl: List<Folkeregisteridentifikator>?,
    barnGrunnlag: List<Folkeregisteridentifikator>?
) = SamsvarMellomPdlOgGrunnlag.Barn(
    fraPdl = barnPdl,
    fraGrunnlag = barnGrunnlag,
    samsvar = barnPdl erLikRekkefoelgeIgnorert barnGrunnlag
)

fun samsvarUtflytting(
    utflyttingPdl: Utland?,
    utflyttingGrunnlag: Utland?
) = SamsvarMellomPdlOgGrunnlag.Utlandsforhold(
    fraPdl = utflyttingPdl,
    fraGrunnlag = utflyttingGrunnlag,
    samsvar = utflyttingPdl?.utflyttingFraNorge erLikRekkefoelgeIgnorert utflyttingGrunnlag?.utflyttingFraNorge &&
        utflyttingPdl?.innflyttingTilNorge erLikRekkefoelgeIgnorert utflyttingGrunnlag?.innflyttingTilNorge

)

infix fun <T> List<T>?.erLikRekkefoelgeIgnorert(other: List<T>?): Boolean =
    this?.size == other?.size && this?.toSet() == other?.toSet()