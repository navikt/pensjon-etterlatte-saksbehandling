package no.nav.etterlatte.pdl.mapper

import no.nav.etterlatte.libs.common.person.Barn
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Foreldre
import no.nav.etterlatte.libs.common.person.ForeldreAnsvar
import no.nav.etterlatte.pdl.PdlForelderBarnRelasjonRolle
import no.nav.etterlatte.pdl.PdlHentPerson

object FamilieRelasjonMapper {

    fun mapFamilieRelasjon(hentPerson: PdlHentPerson): FamilieRelasjon =
        //TODO tar kun med foreldreAnsvar med fnr nå
        //TODO finn ut om det er riktig å hente ut basert på sisteRegistrertDato
        FamilieRelasjon(
            ansvarligeForeldre = hentPerson.foreldreansvar
                ?.filter { it.ansvarlig != null }
                ?.groupBy { it.ansvarlig }
                ?.mapValues { it.value.maxByOrNull { fa -> fa.metadata.sisteRegistrertDato() } }
                ?.map {
                    ForeldreAnsvar(Foedselsnummer.of(it.value?.ansvarlig))
                },

            foreldre = hentPerson.forelderBarnRelasjon
                ?.filter { it.relatertPersonsRolle != PdlForelderBarnRelasjonRolle.BARN }
                ?.groupBy { it.relatertPersonsIdent }
                ?.mapValues { it.value.maxByOrNull { fbr -> fbr.metadata.sisteRegistrertDato() } }
                ?.map {
                    Foreldre(Foedselsnummer.of(it.value?.relatertPersonsIdent))
                },

            barn = hentPerson.forelderBarnRelasjon
                ?.filter { it.relatertPersonsRolle == PdlForelderBarnRelasjonRolle.BARN }
                ?.groupBy { it.relatertPersonsIdent }
                ?.mapValues { it.value.maxByOrNull { fbr -> fbr.metadata.sisteRegistrertDato() } }
                ?.map {
                    Barn(Foedselsnummer.of(it.value?.relatertPersonsIdent))
                }
        )

}