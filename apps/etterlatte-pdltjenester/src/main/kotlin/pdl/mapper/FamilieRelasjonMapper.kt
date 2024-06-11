package no.nav.etterlatte.pdl.mapper

import no.nav.etterlatte.libs.common.behandling.Navn
import no.nav.etterlatte.libs.common.behandling.PersonUtenIdent
import no.nav.etterlatte.libs.common.behandling.RelatertPerson
import no.nav.etterlatte.libs.common.behandling.RelativPersonrolle
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.pdl.PdlForelderAnsvar
import no.nav.etterlatte.pdl.PdlForelderBarnRelasjon
import no.nav.etterlatte.pdl.PdlForelderBarnRelasjonRolle
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlRelatertBiPerson

object FamilieRelasjonMapper {
    fun mapFamilieRelasjon(
        hentPerson: PdlHentPerson,
        personRolle: PersonRolle,
    ): FamilieRelasjon {
        // TODO tar kun med foreldreAnsvar med fnr nå
        // TODO finn ut om det er riktig å hente ut basert på sisteRegistrertDato
        val ansvarligeForeldre =
            when (personRolle) {
                PersonRolle.TILKNYTTET_BARN,
                PersonRolle.BARN,
                ->
                    hentPerson.foreldreansvar
                        ?.filter { it.ansvarlig != null }
                        ?.groupBy { it.ansvarlig }
                        ?.mapValues { it.value.maxByOrNull { fa -> fa.metadata.sisteRegistrertDato() } }
                        ?.mapNotNull {
                            it.value?.ansvarlig?.let { Folkeregisteridentifikator.of(it) }
                        }

                else -> null
            }
        val foreldre =
            when (personRolle) {
                PersonRolle.TILKNYTTET_BARN,
                PersonRolle.BARN,
                ->
                    hentPerson.forelderBarnRelasjon
                        ?.filter { it.relatertPersonsRolle != PdlForelderBarnRelasjonRolle.BARN }
                        ?.groupBy { it.relatertPersonsIdent }
                        ?.mapValues { it.value.maxByOrNull { fbr -> fbr.metadata.sisteRegistrertDato() } }
                        ?.mapNotNull {
                            it.value?.relatertPersonsIdent?.let { Folkeregisteridentifikator.of(it) }
                        }

                else -> null
            }
        val barn =
            when (personRolle) {
                PersonRolle.AVDOED, PersonRolle.GJENLEVENDE ->
                    hentPerson.forelderBarnRelasjon
                        ?.filter { it.relatertPersonsRolle == PdlForelderBarnRelasjonRolle.BARN }
                        ?.groupBy { it.relatertPersonsIdent }
                        ?.mapValues { it.value.maxByOrNull { fbr -> fbr.metadata.sisteRegistrertDato() } }
                        ?.mapNotNull {
                            it.value?.relatertPersonsIdent?.let { Folkeregisteridentifikator.of(it) }
                        }

                else -> null
            }

        val personerUtenIdent = mapPersonerUtenIdenter(hentPerson, personRolle)

        return FamilieRelasjon(
            ansvarligeForeldre = ansvarligeForeldre,
            foreldre = foreldre,
            barn = barn,
            personerUtenIdent = personerUtenIdent.takeUnless { it.isNullOrEmpty() },
        )
    }

    private fun mapForelderBarnUtenIdent(
        forelderBarnRelasjon: List<PdlForelderBarnRelasjon>?,
        personRolle: PersonRolle,
    ): List<PersonUtenIdent> =
        forelderBarnRelasjon
            ?.filter { it.relatertPersonsIdent == null && erRelevantForPersonrolle(it, personRolle) }
            ?.mapNotNull { forelderBarnRelasjoninner ->
                forelderBarnRelasjoninner.relatertPersonUtenFolkeregisteridentifikator
                    ?.tilRelatertPerson()
                    ?.let { forelderBarnRelasjoninner.relatertPersonsRolle to it }
            }?.map { (rolle, person) -> PersonUtenIdent(rolle = rolle.tilRelativPersonrolle(), person = person) }
            ?: emptyList()

    private fun mapForeldreansvarUtenIdent(
        foreldreansvar: List<PdlForelderAnsvar>?,
        personRolle: PersonRolle,
    ): List<PersonUtenIdent> =
        when (personRolle) {
            PersonRolle.BARN ->
                foreldreansvar
                    ?.filter { it.ansvarlig == null }
                    ?.mapNotNull { it.ansvarligUtenIdentifikator?.tilRelatertPerson() }
                    ?.map { PersonUtenIdent(rolle = RelativPersonrolle.FORELDER, person = it) }
                    ?: emptyList()

            else -> emptyList()
        }

    private fun mapPersonerUtenIdenter(
        pdlPerson: PdlHentPerson,
        personRolle: PersonRolle,
    ): List<PersonUtenIdent>? {
        val forelderBarnUtenIdent = mapForelderBarnUtenIdent(pdlPerson.forelderBarnRelasjon, personRolle)
        val foreldreUtenIdent = mapForeldreansvarUtenIdent(pdlPerson.foreldreansvar, personRolle)

        if (forelderBarnUtenIdent.isNotEmpty() || foreldreUtenIdent.isNotEmpty()) {
            return forelderBarnUtenIdent + foreldreUtenIdent
        }
        return null
    }

    private fun erRelevantForPersonrolle(
        forelderBarnRelasjon: PdlForelderBarnRelasjon,
        personrolle: PersonRolle,
    ): Boolean =
        when (forelderBarnRelasjon.relatertPersonsRolle) {
            PdlForelderBarnRelasjonRolle.BARN -> personrolle == PersonRolle.AVDOED || personrolle == PersonRolle.GJENLEVENDE
            PdlForelderBarnRelasjonRolle.FAR,
            PdlForelderBarnRelasjonRolle.MEDMOR,
            PdlForelderBarnRelasjonRolle.MOR,
            -> personrolle == PersonRolle.BARN
        }
}

fun PdlForelderBarnRelasjonRolle.tilRelativPersonrolle(): RelativPersonrolle =
    when (this) {
        PdlForelderBarnRelasjonRolle.BARN -> RelativPersonrolle.BARN
        PdlForelderBarnRelasjonRolle.FAR,
        PdlForelderBarnRelasjonRolle.MEDMOR,
        PdlForelderBarnRelasjonRolle.MOR,
        -> RelativPersonrolle.FORELDER
    }

fun PdlRelatertBiPerson.tilRelatertPerson(): RelatertPerson =
    RelatertPerson(
        foedselsdato = this.foedselsdato,
        kjoenn = this.kjoenn,
        navn =
            this.navn?.let {
                Navn(it.fornavn, it.mellomnavn, it.etternavn)
            },
        statsborgerskap = this.statsborgerskap,
    )
