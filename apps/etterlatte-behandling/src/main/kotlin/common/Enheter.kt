package no.nav.etterlatte.common

import no.nav.etterlatte.libs.common.person.Foedselsnummer

enum class Enheter(val enhetNr: String, val navn: String) {
    STRENGT_FORTROLIG("2103", "NAV Vikafossen"),
    STRENGT_FORTROLIG_UTLAND("2103", "NAV Vikafossen"),
    FORTROLIG("????", ""),
    EGEN_ANSATTE("4883", "NAV Familie- og pensjonsytelser Egne ansatte"),
    DEFAULT("4808", "NAV Familie- og pensjonsytelser Porsgrunn")
}

open class EnhetException(override val message: String) : Exception(message)

class IngenEnhetFunnetException(val omraade: String, val tema: String) : EnhetException(
    message = "Ingen enheter funnet for tema $tema og omraade $omraade"
)
class IngenGeografiskOmraadeFunnetForEnhet(val person: Foedselsnummer, val tema: String) : EnhetException(
    message = "Fant ikke geografisk omraade for $person og tema $tema"
)