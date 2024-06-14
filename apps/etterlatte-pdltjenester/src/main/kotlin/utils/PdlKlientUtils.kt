package no.nav.etterlatte.utils

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.pdl.Criteria
import no.nav.etterlatte.pdl.Paging
import no.nav.etterlatte.pdl.PdlSoekVariables
import no.nav.etterlatte.pdl.PdlVariables
import no.nav.etterlatte.pdl.SearchRule
import no.nav.etterlatte.pdl.SoekPerson

fun toPdlVariables(
    fnr: Folkeregisteridentifikator,
    rolle: PersonRolle,
) = when (rolle) {
    PersonRolle.INNSENDER ->
        PdlVariables(
            ident = fnr.value,
            bostedsadresse = true,
            bostedsadresseHistorikk = false,
            deltBostedsadresse = false,
            kontaktadresse = false,
            kontaktadresseHistorikk = false,
            oppholdsadresse = false,
            oppholdsadresseHistorikk = false,
            utland = false,
            sivilstand = false,
            familieRelasjon = false,
            vergemaal = false,
        )

    PersonRolle.BARN ->
        PdlVariables(
            ident = fnr.value,
            bostedsadresse = true,
            bostedsadresseHistorikk = true,
            deltBostedsadresse = true,
            kontaktadresse = true,
            kontaktadresseHistorikk = true,
            oppholdsadresse = true,
            oppholdsadresseHistorikk = true,
            utland = true,
            sivilstand = false,
            familieRelasjon = true,
            vergemaal = true,
        )

    PersonRolle.GJENLEVENDE ->
        PdlVariables(
            ident = fnr.value,
            bostedsadresse = true,
            bostedsadresseHistorikk = true,
            deltBostedsadresse = false,
            kontaktadresse = false,
            kontaktadresseHistorikk = false,
            oppholdsadresse = true,
            oppholdsadresseHistorikk = false,
            utland = true,
            sivilstand = true,
            sivilstandHistorikk = true,
            familieRelasjon = true,
            vergemaal = true,
        )

    PersonRolle.AVDOED ->
        PdlVariables(
            ident = fnr.value,
            bostedsadresse = true,
            bostedsadresseHistorikk = true,
            deltBostedsadresse = false,
            kontaktadresse = true,
            kontaktadresseHistorikk = true,
            oppholdsadresse = true,
            oppholdsadresseHistorikk = true,
            utland = true,
            sivilstand = true,
            sivilstandHistorikk = true,
            familieRelasjon = true,
            vergemaal = false,
        )

    PersonRolle.TILKNYTTET_BARN ->
        PdlVariables(
            ident = fnr.value,
            bostedsadresse = true,
            bostedsadresseHistorikk = true,
            deltBostedsadresse = true,
            kontaktadresse = true,
            kontaktadresseHistorikk = true,
            oppholdsadresse = true,
            oppholdsadresseHistorikk = true,
            utland = true,
            sivilstand = false,
            familieRelasjon = false,
            vergemaal = false,
        )
}

fun toPdlSearchVariables(soekPerson: SoekPerson): PdlSoekVariables {
    val criterias = mutableListOf<Criteria>()
    soekPerson.fornavn?.let {
        criterias.add(Criteria("person.navn.fornavn", SearchRule.EQUALS.keyName, it))
    }
    soekPerson.etternavn?.let {
        criterias.add(Criteria("person.navn.etternavn", SearchRule.EQUALS.keyName, it))
    }
    soekPerson.foedselsdato?.let {
        criterias.add(Criteria("person.foedseldato.foedselsdato", SearchRule.EQUALS.keyName, it))
    }

    return PdlSoekVariables(
        paging = Paging(),
        criteria = criterias,
    )
}
