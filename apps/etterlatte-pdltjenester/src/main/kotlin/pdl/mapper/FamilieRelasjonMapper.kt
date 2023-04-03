package no.nav.etterlatte.pdl.mapper

import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.FamilieRelasjonManglerIdent
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.pdl.PdlForelderBarnRelasjonRolle
import no.nav.etterlatte.pdl.PdlHentPerson

object FamilieRelasjonMapper {

    fun mapFamilieRelasjon(hentPerson: PdlHentPerson, personRolle: PersonRolle): FamilieRelasjon =
        // TODO tar kun med foreldreAnsvar med fnr nå
        // TODO finn ut om det er riktig å hente ut basert på sisteRegistrertDato
        FamilieRelasjon(
            ansvarligeForeldre = when (personRolle) {
                PersonRolle.BARN ->
                    hentPerson.foreldreansvar
                        ?.filter { it.ansvarlig != null }
                        ?.groupBy { it.ansvarlig }
                        ?.mapValues { it.value.maxByOrNull { fa -> fa.metadata.sisteRegistrertDato() } }
                        ?.map {
                            it.value?.ansvarlig?.let { Folkeregisteridentifikator.of(it) }
                                ?: throw FamilieRelasjonManglerIdent("${it.value} mangler ident")
                        }

                else -> null
            },
            foreldre = when (personRolle) {
                PersonRolle.BARN ->
                    hentPerson.forelderBarnRelasjon
                        ?.filter { it.relatertPersonsRolle != PdlForelderBarnRelasjonRolle.BARN }
                        ?.groupBy { it.relatertPersonsIdent }
                        ?.mapValues { it.value.maxByOrNull { fbr -> fbr.metadata.sisteRegistrertDato() } }
                        ?.map {
                            it.value?.relatertPersonsIdent?.let { Folkeregisteridentifikator.of(it) }
                                ?: throw FamilieRelasjonManglerIdent("${it.value} mangler ident")
                        }

                else -> null
            },
            barn = when (personRolle) {
                PersonRolle.AVDOED, PersonRolle.GJENLEVENDE ->
                    hentPerson.forelderBarnRelasjon
                        ?.filter { it.relatertPersonsRolle == PdlForelderBarnRelasjonRolle.BARN }
                        ?.groupBy { it.relatertPersonsIdent }
                        ?.mapValues { it.value.maxByOrNull { fbr -> fbr.metadata.sisteRegistrertDato() } }
                        ?.map {
                            it.value?.relatertPersonsIdent?.let { Folkeregisteridentifikator.of(it) }
                                ?: throw FamilieRelasjonManglerIdent("${it.value} mangler ident")
                        }

                else -> null
            }
        )
}