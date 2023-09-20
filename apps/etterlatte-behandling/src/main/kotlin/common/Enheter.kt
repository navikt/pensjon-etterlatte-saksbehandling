package no.nav.etterlatte.common

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator

enum class Enheter(val enhetNr: String, val navn: String, val inkludertForNasjonalTilgang: Boolean) {
    STRENGT_FORTROLIG("2103", "NAV Vikafossen", false),
    STRENGT_FORTROLIG_UTLAND("2103", "NAV Vikafossen", false),
    EGNE_ANSATTE("4883", "NAV Familie- og pensjonsytelser Egne ansatte", false),
    UTLAND("0001", "NAV Familie- og pensjonsytelser Utland", false),
    AALESUND("4815", "NAV Familie- og pensjonsytelser Ålesund", true),
    AALESUND_UTLAND("4862", "NAV Familie- og pensjonsytelser Ålesund Utland", false),
    STEINKJER("4817", "NAV Familie- og pensjonsytelser Steinkjer", true),
    PORSGRUNN("4808", "NAV Familie- og pensjonsytelser Porsgrunn", true),
    ;

    companion object {
        val defaultEnhet = PORSGRUNN

        fun nasjonalTilgangEnheter() = Enheter.values().filter { it.inkludertForNasjonalTilgang }.map { it.enhetNr }
    }
}

open class EnhetException(override val message: String) : Exception(message)

class IngenEnhetFunnetException(val omraade: String, val tema: String) : EnhetException(
    message = "Ingen enheter funnet for tema $tema og omraade $omraade",
)

class IngenGeografiskOmraadeFunnetForEnhet(val person: Folkeregisteridentifikator, val tema: String) : EnhetException(
    message = "Fant ikke geografisk omraade for $person og tema $tema",
)
