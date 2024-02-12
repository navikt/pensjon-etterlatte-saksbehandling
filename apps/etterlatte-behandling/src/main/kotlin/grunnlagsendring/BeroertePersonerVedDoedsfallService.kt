package no.nav.etterlatte.grunnlagsendring

import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class BeroertePersonerVedDoedsfallService(private val pdlTjenesterKlient: PdlTjenesterKlient) {
    // Finner beroerte personer basert på regler beskrevet i https://confluence.adeo.no/display/TE/Funksjonelle+krav
    fun hentBeroertePersoner(fnr: String): List<Person> {
        val avdoed = pdlTjenesterKlient.hentPdlModell(fnr, PersonRolle.AVDOED, SakType.BARNEPENSJON)

        checkNotNull(avdoed.doedsdato) { "Personen er ikke død" }

        return finnBeroerteBarn(avdoed)
    }

    private fun finnBeroerteBarn(avdoed: PersonDTO): List<Person> {
        val maanedenEtterDoedsfall = avdoed.doedsdato!!.verdi.plusMonths(1).withDayOfMonth(1)

        return with(avdoed.avdoedesBarn ?: emptyList()) {
            this.filter { barn -> barn.doedsdato == null }
                .filter { barn -> barn.under20PaaDato(maanedenEtterDoedsfall) }
        }
    }
}

private fun Person.under20PaaDato(dato: LocalDate): Boolean =
    if (foedselsdato != null) {
        ChronoUnit.YEARS.between(foedselsdato, dato) < 20
    } else {
        foedselsnummer.getAgeAtDate(dato) < 20
    }
