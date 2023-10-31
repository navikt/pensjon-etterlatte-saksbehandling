package no.nav.etterlatte.pdl.mapper

import no.nav.etterlatte.libs.common.behandling.Navn
import no.nav.etterlatte.libs.common.behandling.PersonUtenIdent
import no.nav.etterlatte.libs.common.behandling.RelatertPerson
import no.nav.etterlatte.libs.common.behandling.RelativPersonrolle
import no.nav.etterlatte.libs.common.pdl.IngenIdentFamilierelasjonException
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.pdl.PdlForelderBarnRelasjon
import no.nav.etterlatte.pdl.PdlForelderBarnRelasjonRolle
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlRelatertBiPerson
import no.nav.etterlatte.sikkerLogg
import org.slf4j.LoggerFactory

object FamilieRelasjonMapper {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun mapFamilieRelasjon(
        hentPerson: PdlHentPerson,
        personRolle: PersonRolle,
        aksepterPersonerUtenIdent: Boolean,
    ): FamilieRelasjon {
        // TODO tar kun med foreldreAnsvar med fnr nå
        // TODO finn ut om det er riktig å hente ut basert på sisteRegistrertDato
        val ansvarligeForeldre =
            when (personRolle) {
                PersonRolle.BARN ->
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
                PersonRolle.BARN ->
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

        if (personerUtenIdent.isNullOrEmpty()) {
            return FamilieRelasjon(ansvarligeForeldre, foreldre, barn)
        }

        if (!aksepterPersonerUtenIdent) {
            logger.error(
                "Feilet i mapping av familierelasjon på grunn av personer som mangler identer. " +
                    "Se sikkerlogg for detaljer.",
            )
            sikkerLogg.error(
                "Feilet i mapping av familierelasjon på grunn av personer som mangler identer. " +
                    "Mappet følgende personer som mangler identer: $personerUtenIdent",
            )
            throw IngenIdentFamilierelasjonException()
        }

        return FamilieRelasjon(
            ansvarligeForeldre = ansvarligeForeldre,
            foreldre = foreldre,
            barn = barn,
            personerUtenIdent = personerUtenIdent,
        )
    }

    private fun mapPersonerUtenIdenter(
        hentPerson: PdlHentPerson,
        personRolle: PersonRolle,
    ): List<PersonUtenIdent>? {
        val forelderBarnUtenIdent =
            hentPerson.forelderBarnRelasjon
                ?.filter { it.relatertPersonsIdent == null && erRelevantForPersonrolle(it, personRolle) }
                ?.mapNotNull { forelderBarnRelasjon ->
                    forelderBarnRelasjon.relatertPersonUtenFolkeregisteridentifikator?.tilRelatertPerson()
                        ?.let { forelderBarnRelasjon.relatertPersonsRolle to it }
                }
                ?.map { (rolle, person) -> PersonUtenIdent(rolle = rolle.tilRelativPersonrolle(), person = person) }
                ?: emptyList()

        val foreldreUtenIdent =
            if (personRolle == PersonRolle.BARN) {
                hentPerson.foreldreansvar
                    ?.filter { it.ansvarlig == null }
                    ?.mapNotNull { it.ansvarligUtenIdentifikator?.tilRelatertPerson() }
                    ?.map { PersonUtenIdent(rolle = RelativPersonrolle.FORELDER, person = it) }
                    ?: emptyList()
            } else {
                emptyList()
            }

        if (forelderBarnUtenIdent.isNotEmpty() || foreldreUtenIdent.isNotEmpty()) {
            return forelderBarnUtenIdent + foreldreUtenIdent
        }
        return null
    }

    private fun erRelevantForPersonrolle(
        forelderBarnRelasjon: PdlForelderBarnRelasjon,
        personrolle: PersonRolle,
    ): Boolean {
        return when (forelderBarnRelasjon.relatertPersonsRolle) {
            PdlForelderBarnRelasjonRolle.BARN -> personrolle == PersonRolle.AVDOED || personrolle == PersonRolle.GJENLEVENDE
            PdlForelderBarnRelasjonRolle.FAR,
            PdlForelderBarnRelasjonRolle.MEDMOR,
            PdlForelderBarnRelasjonRolle.MOR,
            -> personrolle == PersonRolle.BARN
        }
    }
}

fun PdlForelderBarnRelasjonRolle.tilRelativPersonrolle(): RelativPersonrolle {
    return when (this) {
        PdlForelderBarnRelasjonRolle.BARN -> RelativPersonrolle.BARN
        PdlForelderBarnRelasjonRolle.FAR,
        PdlForelderBarnRelasjonRolle.MEDMOR,
        PdlForelderBarnRelasjonRolle.MOR,
        -> RelativPersonrolle.FORELDER
    }
}

fun PdlRelatertBiPerson.tilRelatertPerson(): RelatertPerson {
    return RelatertPerson(
        foedselsdato = this.foedselsdato,
        kjoenn = this.kjoenn,
        navn =
            this.navn?.let {
                Navn(it.fornavn, it.mellomnavn, it.etternavn)
            },
        statsborgerskap = this.statsborgerskap,
    )
}
