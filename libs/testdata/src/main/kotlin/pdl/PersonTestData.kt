package no.nav.etterlatte.libs.testdata.pdl

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.hentAdressebeskyttelse
import no.nav.etterlatte.libs.common.grunnlag.hentAvdoedesbarn
import no.nav.etterlatte.libs.common.grunnlag.hentBostedsadresse
import no.nav.etterlatte.libs.common.grunnlag.hentDeltbostedsadresse
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFamilierelasjon
import no.nav.etterlatte.libs.common.grunnlag.hentFoedeland
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsaar
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentKontaktadresse
import no.nav.etterlatte.libs.common.grunnlag.hentNavn
import no.nav.etterlatte.libs.common.grunnlag.hentOppholdsadresse
import no.nav.etterlatte.libs.common.grunnlag.hentSivilstand
import no.nav.etterlatte.libs.common.grunnlag.hentSivilstatus
import no.nav.etterlatte.libs.common.grunnlag.hentStatsborgerskap
import no.nav.etterlatte.libs.common.grunnlag.hentUtland
import no.nav.etterlatte.libs.common.grunnlag.hentVergemaalellerfremtidsfullmakt
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Person

fun personTestData(
    opplysningsmap: Map<Opplysningstype, Opplysning<JsonNode>>
): Person = Person(
    fornavn = opplysningsmap.hentNavn()!!.verdi.fornavn,
    etternavn = opplysningsmap.hentNavn()!!.verdi.etternavn,
    foedselsnummer = opplysningsmap.hentFoedselsnummer()!!.verdi,
    foedselsdato = opplysningsmap.hentFoedselsdato()!!.verdi,
    foedselsaar = opplysningsmap.hentFoedselsaar()!!.verdi,
    foedeland = opplysningsmap.hentFoedeland()?.verdi,
    doedsdato = opplysningsmap.hentDoedsdato()?.verdi,
    adressebeskyttelse = opplysningsmap.hentAdressebeskyttelse()?.verdi,
    bostedsadresse = opplysningsmap.hentBostedsadresse()?.perioder?.map { it.verdi },
    deltBostedsadresse = opplysningsmap.hentDeltbostedsadresse()?.verdi,
    kontaktadresse = opplysningsmap.hentKontaktadresse()?.verdi,
    oppholdsadresse = opplysningsmap.hentOppholdsadresse()?.verdi,
    sivilstatus = opplysningsmap.hentSivilstatus()?.verdi,
    sivilstand = opplysningsmap.hentSivilstand()?.verdi,
    statsborgerskap = opplysningsmap.hentStatsborgerskap()?.verdi,
    utland = opplysningsmap.hentUtland()?.verdi,
    familieRelasjon = opplysningsmap.hentFamilierelasjon()?.verdi,
    avdoedesBarn = opplysningsmap.hentAvdoedesbarn()?.verdi?.avdoedesBarn,
    vergemaalEllerFremtidsfullmakt = opplysningsmap.hentVergemaalellerfremtidsfullmakt()?.verdi
)