package no.nav.etterlatte.common

enum class Enheter(
    val enhetNr: String,
    val navn: String,
    val inkludertForNasjonalTilgang: Boolean,
    val skrivetilgang: Boolean,
) {
    STRENGT_FORTROLIG("2103", "NAV Vikafossen", false, true),
    STRENGT_FORTROLIG_UTLAND("2103", "NAV Vikafossen", false, true),
    EGNE_ANSATTE("4883", "NAV Familie- og pensjonsytelser Egne ansatte", false, true),
    UTLAND("0001", "NAV Familie- og pensjonsytelser Utland", true, true),
    AALESUND("4815", "NAV Familie- og pensjonsytelser Ålesund", true, true),
    AALESUND_UTLAND("4862", "NAV Familie- og pensjonsytelser Ålesund Utland", true, true),
    STEINKJER("4817", "NAV Familie- og pensjonsytelser Steinkjer", true, true),
    PORSGRUNN("4808", "NAV Familie- og pensjonsytelser Porsgrunn", true, true),
    OEST_VIKEN("4101", "NAV Kontaktsenter Øst-Viken", false, false),
    TROENDELAG("4116", "NAV Kontaktsenter Trøndelag", false, false),
    NORDLAND_BODOE("4118", "NAV Kontaktsenter Nordland Bodø", false, false),
    NOEP("4819", "NAV Økonomi Pensjon", false, false),
    KLAGE_VEST("4294", "NAV Klageinstans vest", false, false),
    ;

    companion object {
        val defaultEnhet = PORSGRUNN

        fun nasjonalTilgangEnheter() = entries.filter { it.inkludertForNasjonalTilgang }.map { it.enhetNr }

        fun finnEnhetForEnhetsnummer(enhetNr: String) = entries.firstOrNull { it.enhetNr == enhetNr }

        fun enheterMedSkrivetilgang() = entries.filter { it.skrivetilgang }.map { it.enhetNr }.toSet()
    }
}

open class EnhetException(override val message: String) : Exception(message)

class IngenEnhetFunnetException(val omraade: String, val tema: String) : EnhetException(
    message = "Ingen enheter funnet for tema $tema og omraade $omraade",
)
