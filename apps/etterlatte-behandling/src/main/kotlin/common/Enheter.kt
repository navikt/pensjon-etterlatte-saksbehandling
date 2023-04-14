package no.nav.etterlatte.common

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator

enum class Enheter(val enhetNr: String, val navn: String) {
    FORTROLIG("2103", "NAV Vikafossen"),
    STRENGT_FORTROLIG("2103", "NAV Vikafossen"),
    STRENGT_FORTROLIG_UTLAND("2103", "NAV Vikafossen"),
    EGNE_ANSATTE("4883", "NAV Familie- og pensjonsytelser Egne ansatte"),
    DEFAULT_PORSGRUNN("4808", "NAV Familie- og pensjonsytelser Porsgrunn")
}

open class EnhetException(override val message: String) : Exception(message)

class IngenEnhetFunnetException(val omraade: String, val tema: String) : EnhetException(
    message = "Ingen enheter funnet for tema $tema og omraade $omraade"
)

class IngenGeografiskOmraadeFunnetForEnhet(val person: Folkeregisteridentifikator, val tema: String) : EnhetException(
    message = "Fant ikke geografisk omraade for $person og tema $tema"
)