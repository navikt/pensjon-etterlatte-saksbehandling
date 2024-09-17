package no.nav.etterlatte.common

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException

enum class Enhet(
    @JsonValue val enhetNr: String,
    val navn: String,
    val vanligSaksbehandlerEnhet: Boolean,
    val erSaksbehandlendeEnhet: Boolean,
    val harTilgangTilOppgavebenken: Boolean,
) {
    STRENGT_FORTROLIG("2103", "NAV Vikafossen", false, true, true),
    STRENGT_FORTROLIG_UTLAND("2103", "NAV Vikafossen", false, true, true),
    EGNE_ANSATTE("4883", "NAV Familie- og pensjonsytelser Egne ansatte", false, true, true),
    UTLAND("0001", "NAV Familie- og pensjonsytelser Utland", true, true, true),
    AALESUND("4815", "NAV Familie- og pensjonsytelser Ålesund", true, true, true),
    AALESUND_UTLAND("4862", "NAV Familie- og pensjonsytelser Ålesund Utland", true, true, true),
    STEINKJER("4817", "NAV Familie- og pensjonsytelser Steinkjer", true, true, true),
    PORSGRUNN("4808", "NAV Familie- og pensjonsytelser Porsgrunn", true, true, true),
    OEST_VIKEN("4101", "NAV Kontaktsenter Øst-Viken", false, false, false),
    TROENDELAG("4116", "NAV Kontaktsenter Trøndelag", false, false, false),
    NORDLAND_BODOE("4118", "NAV Kontaktsenter Nordland Bodø", false, false, false),
    NOEP("4819", "NAV Økonomi Pensjon", false, false, false),
    KLAGE_VEST("4294", "NAV Klageinstans vest", false, false, false),

    ;

    companion object {
        val defaultEnhet = PORSGRUNN

        fun enheterForVanligSaksbehandlere() = entries.filter { it.vanligSaksbehandlerEnhet }.map { it.enhetNr }

        fun saksbehandlendeEnheter() = entries.filter { it.erSaksbehandlendeEnhet }.map { it.enhetNr }.toSet()

        fun kjenteEnheter() = entries.map { it.enhetNr }.toSet()

        fun fraEnhetNr(enhetNr: String): Enhet =
            entries.firstOrNull { it.enhetNr == enhetNr }
                ?: throw UgyldigForespoerselException(
                    code = "ENHET IKKE GYLDIG",
                    detail = "enhet $enhetNr er ikke i listen over gyldige enheter",
                )
    }
}
