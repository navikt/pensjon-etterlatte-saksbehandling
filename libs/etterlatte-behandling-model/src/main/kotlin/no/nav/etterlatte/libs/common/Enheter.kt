package no.nav.etterlatte.common

import no.nav.etterlatte.libs.common.Enhetsnummer

enum class Enheter(
    val enhetNr: Enhetsnummer,
    val navn: String,
    val vanligSaksbehandlerEnhet: Boolean,
    val erSaksbehandlendeEnhet: Boolean,
    val harTilgangTilOppgavebenken: Boolean,
) {
    STRENGT_FORTROLIG(Enhetsnummer("2103"), "NAV Vikafossen", false, true, true),
    STRENGT_FORTROLIG_UTLAND(Enhetsnummer("2103"), "NAV Vikafossen", false, true, true),
    EGNE_ANSATTE(Enhetsnummer("4883"), "NAV Familie- og pensjonsytelser Egne ansatte", false, true, true),
    UTLAND(Enhetsnummer("0001"), "NAV Familie- og pensjonsytelser Utland", true, true, true),
    AALESUND(Enhetsnummer("4815"), "NAV Familie- og pensjonsytelser Ålesund", true, true, true),
    AALESUND_UTLAND(Enhetsnummer("4862"), "NAV Familie- og pensjonsytelser Ålesund Utland", true, true, true),
    STEINKJER(Enhetsnummer("4817"), "NAV Familie- og pensjonsytelser Steinkjer", true, true, true),
    PORSGRUNN(Enhetsnummer("4808"), "NAV Familie- og pensjonsytelser Porsgrunn", true, true, true),
    OEST_VIKEN(Enhetsnummer("4101"), "NAV Kontaktsenter Øst-Viken", false, false, false),
    TROENDELAG(Enhetsnummer("4116"), "NAV Kontaktsenter Trøndelag", false, false, false),
    NORDLAND_BODOE(Enhetsnummer("4118"), "NAV Kontaktsenter Nordland Bodø", false, false, false),
    NOEP(Enhetsnummer("4819"), "NAV Økonomi Pensjon", false, false, false),
    KLAGE_VEST(Enhetsnummer("4294"), "NAV Klageinstans vest", false, false, false),

    ;

    companion object {
        val defaultEnhet = PORSGRUNN

        fun enheterForVanligSaksbehandlere() = entries.filter { it.vanligSaksbehandlerEnhet }.map { it.enhetNr }

        fun saksbehandlendeEnheter() = entries.filter { it.erSaksbehandlendeEnhet }.map { it.enhetNr }.toSet()

        fun kjenteEnheter() = entries.map { it.enhetNr }.toSet()
    }
}
