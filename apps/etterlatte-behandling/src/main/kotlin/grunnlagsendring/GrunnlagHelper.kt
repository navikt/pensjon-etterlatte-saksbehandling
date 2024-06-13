package no.nav.etterlatte.grunnlagsendring

import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentBostedsadresse
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFamilierelasjon
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentSivilstand
import no.nav.etterlatte.libs.common.grunnlag.hentSoekerPdlV1
import no.nav.etterlatte.libs.common.grunnlag.hentUtland

class GrunnlagRolleException(
    override val message: String,
) : Exception(message)

fun Grunnlag.doedsdato(
    saksrolle: Saksrolle,
    fnr: String,
) = when (saksrolle) {
    Saksrolle.AVDOED -> {
        hentAvdoede().find { it.hentFoedselsnummer()?.verdi?.value == fnr }?.hentDoedsdato()
    }
    Saksrolle.GJENLEVENDE -> {
        hentPotensiellGjenlevende()?.hentDoedsdato()
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

fun Grunnlag.bostedsadresse(
    saksrolle: Saksrolle,
    fnr: String,
) = when (saksrolle) {
    Saksrolle.AVDOED -> {
        hentAvdoede().find { it.hentFoedselsnummer()?.verdi?.value == fnr }?.hentBostedsadresse()
    }

    Saksrolle.GJENLEVENDE -> {
        hentPotensiellGjenlevende()?.hentBostedsadresse()
    }

    Saksrolle.SOEKER -> {
        soeker.hentBostedsadresse()
    }

    Saksrolle.SOESKEN -> {
        hentSoesken().find { it.hentFoedselsnummer()?.verdi?.value == fnr }?.hentBostedsadresse()
    }

    else -> throw GrunnlagRolleException(
        "Proevde aa finne bostedsadresse for $saksrolle, men det skal ikke kunne skje",
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
        hentSoesken()
            .find { it.hentFoedselsnummer()?.verdi?.value == fnr }
            ?.hentFamilierelasjon()
            ?.verdi
            ?.ansvarligeForeldre
    }
    else -> throw GrunnlagRolleException(
        "Proevde aa finne ansvarligeForeldre for $saksrolle, men det skal ikke kunne skje",
    )
}

fun Grunnlag.barn(saksrolle: Saksrolle) =
    when (saksrolle) {
        Saksrolle.AVDOED -> {
            hentAvdoede().flatMap { it.hentFamilierelasjon()?.verdi?.barn ?: emptyList() }.ifEmpty { null }
        }
        Saksrolle.GJENLEVENDE -> {
            hentPotensiellGjenlevende()?.hentFamilierelasjon()?.verdi?.barn
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
        hentAvdoede().find { it.hentFoedselsnummer()?.verdi?.value == fnr }?.hentUtland()?.verdi
    }
    Saksrolle.GJENLEVENDE -> {
        hentPotensiellGjenlevende()?.hentUtland()?.verdi
    }
    else -> throw GrunnlagRolleException(
        "Proevde aa finne utland for $saksrolle, men det skal ikke kunne skje",
    )
}

fun Grunnlag.vergemaalellerfremtidsfullmakt(saksrolle: Saksrolle) =
    when (saksrolle) {
        Saksrolle.SOEKER -> {
            soeker.hentSoekerPdlV1()?.verdi?.vergemaalEllerFremtidsfullmakt
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
            hentPotensiellGjenlevende()?.hentSivilstand()?.verdi
        }
        Saksrolle.AVDOED -> {
            // first er helt ok her, siden vi kun er interessert når vi er i OMS land og skal da ha kun
            // en avdød per behandling.
            hentAvdoede().first().hentSivilstand()?.verdi
        }
        else -> throw GrunnlagRolleException(
            "Prøvde å finne sivilstand for $saksrolle, men det er ikke relevant for denne rollen",
        )
    }
