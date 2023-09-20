package no.nav.etterlatte.grunnlagsendring

import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFamilierelasjon
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentSivilstand
import no.nav.etterlatte.libs.common.grunnlag.hentUtland
import no.nav.etterlatte.libs.common.grunnlag.hentVergemaalellerfremtidsfullmakt

class GrunnlagRolleException(override val message: String) : Exception(message)

fun Grunnlag.doedsdato(
    saksrolle: Saksrolle,
    fnr: String,
) = when (saksrolle) {
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
    else -> throw GrunnlagRolleException(
        "Proevde aa finne doedsdato for $saksrolle, men det skal ikke kunne skje",
    )
}

fun Grunnlag.ansvarligeForeldre(
    saksrolle: Saksrolle,
    fnr: String,
) = when (saksrolle) {
    Saksrolle.SOEKER -> {
        soeker.hentFamilierelasjon()?.verdi?.ansvarligeForeldre
    }
    Saksrolle.SOESKEN -> {
        hentSoesken().find { it.hentFoedselsnummer()?.verdi?.value == fnr }
            ?.hentFamilierelasjon()?.verdi?.ansvarligeForeldre
    }
    else -> throw GrunnlagRolleException(
        "Proevde aa finne ansvarligeForeldre for $saksrolle, men det skal ikke kunne skje",
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
        Saksrolle.SOEKER -> {
            soeker.hentFamilierelasjon()?.verdi?.barn
        }
        else -> throw GrunnlagRolleException(
            "Proevde aa finne barn for $saksrolle, men det skal ikke kunne skje",
        )
    }

fun Grunnlag.utland(
    saksrolle: Saksrolle,
    fnr: String,
) = when (saksrolle) {
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
    else -> throw GrunnlagRolleException(
        "Proevde aa finne utland for $saksrolle, men det skal ikke kunne skje",
    )
}

fun Grunnlag.vergemaalellerfremtidsfullmakt(saksrolle: Saksrolle) =
    when (saksrolle) {
        Saksrolle.SOEKER -> {
            soeker.hentVergemaalellerfremtidsfullmakt()?.verdi
        }
        else -> throw GrunnlagRolleException(
            "Prøvde å finne vergemål på en person som ikke er søker, men det er ikke relevant",
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
        else -> throw GrunnlagRolleException(
            "Prøvde å finne sivilstand for $saksrolle, men det er ikke relevant for denne rollen",
        )
    }
