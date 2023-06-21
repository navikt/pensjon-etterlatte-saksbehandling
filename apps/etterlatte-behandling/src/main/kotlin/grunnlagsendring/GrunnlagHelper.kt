package no.nav.etterlatte.grunnlagsendring

import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFamilierelasjon
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentSivilstand
import no.nav.etterlatte.libs.common.grunnlag.hentUtland
import no.nav.etterlatte.libs.common.grunnlag.hentVergemaalellerfremtidsfullmakt

fun Grunnlag.doedsdato(saksrolle: Saksrolle, fnr: String) =
    when (saksrolle) {
        Saksrolle.AVDOED -> {
            hentAvdoed().hentDoedsdato()
        }
        Saksrolle.GJENLEVENDE -> {
            hentGjenlevende().hentDoedsdato()
        }
        Saksrolle.SOEKER -> {
            soeker.hentDoedsdato()
        }
        Saksrolle.SOESKEN -> {
            hentSoesken().find { it.hentFoedselsnummer()?.verdi?.value == fnr }?.hentDoedsdato()
        }
        else -> throw IllegalArgumentException(
            "Proevde aa finne doedsdato for $saksrolle, men det skal ikke kunne skje D:"
        )
    }

fun Grunnlag.ansvarligeForeldre(saksrolle: Saksrolle, fnr: String) =
    when (saksrolle) {
        Saksrolle.SOEKER -> {
            soeker.hentFamilierelasjon()?.verdi?.ansvarligeForeldre
        }
        Saksrolle.SOESKEN -> {
            hentSoesken().find { it.hentFoedselsnummer()?.verdi?.value == fnr }
                ?.hentFamilierelasjon()?.verdi?.ansvarligeForeldre
        }
        else -> throw IllegalArgumentException(
            "Proevde aa finne doedsdato for $saksrolle, men det skal ikke kunne skje D:"
        )
    }

fun Grunnlag.barn(saksrolle: Saksrolle) =
    when (saksrolle) {
        Saksrolle.AVDOED -> {
            hentAvdoed().hentFamilierelasjon()?.verdi?.barn
        }
        Saksrolle.GJENLEVENDE -> {
            hentGjenlevende().hentFamilierelasjon()?.verdi?.barn
        }
        else -> throw IllegalArgumentException(
            "Proevde aa finne doedsdato for $saksrolle, men det skal ikke kunne skje D:"
        )
    }

fun Grunnlag.utland(saksrolle: Saksrolle, fnr: String) =
    when (saksrolle) {
        Saksrolle.SOEKER -> {
            soeker.hentUtland()?.verdi
        }
        Saksrolle.SOESKEN -> {
            hentSoesken().find { it.hentFoedselsnummer()?.verdi?.value == fnr }?.hentUtland()?.verdi
        }
        Saksrolle.AVDOED -> {
            hentAvdoed().hentUtland()?.verdi
        }
        Saksrolle.GJENLEVENDE -> {
            hentGjenlevende().hentUtland()?.verdi
        }
        else -> throw IllegalArgumentException(
            "Proevde aa finne doedsdato for $saksrolle, men det skal ikke kunne skje D:"
        )
    }

fun Grunnlag.vergemaalellerfremtidsfullmakt(saksrolle: Saksrolle) =
    when (saksrolle) {
        Saksrolle.SOEKER -> {
            soeker.hentVergemaalellerfremtidsfullmakt()?.verdi
        }
        else -> throw IllegalArgumentException(
            "Prøvde å finne vergemål på en person som ikke er søker, men det er ikke relevant"
        )
    }

fun Grunnlag.sivilstand(saksrolle: Saksrolle) =
    when (saksrolle) {
        Saksrolle.SOEKER -> {
            soeker.hentSivilstand()?.verdi
        }
        Saksrolle.GJENLEVENDE -> {
            hentGjenlevende().hentSivilstand()?.verdi
        }
        Saksrolle.AVDOED -> {
            hentAvdoed().hentSivilstand()?.verdi
        }
        else -> throw IllegalArgumentException(
            "Prøvde å finne sivilstand for $saksrolle, men det er ikke relevant for denne rollen"
        )
    }