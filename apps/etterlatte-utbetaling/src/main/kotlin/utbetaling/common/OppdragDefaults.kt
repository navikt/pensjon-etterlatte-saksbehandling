package no.nav.etterlatte.utbetaling.common

import no.trygdeetaten.skjema.oppdrag.OppdragsEnhet120
import no.trygdeetaten.skjema.oppdrag.TfradragTillegg
import java.time.LocalDate

object OppdragDefaults {
    const val AVLEVERENDE_KOMPONENTKODE = "ETTERLAT" // felles for alle etterlatteytelser

    // https://nav-it.slack.com/archives/C03793FA1EE/p1649233182935959
    const val MOTTAKENDE_KOMPONENTKODE = "OS" // Oppdragsystemet
    const val SAKSBEHANDLER_ID = "" // TODO: høre med økonomi om denne skal brukes og hva den skal være
    const val KODE_KOMPONENT = ""
    const val UTBETALINGSFREKVENS = "MND"
    const val KJOEREPLAN_SAMMEN_MED_NESTE_PLANLAGTE_UTBETALING = "J"
    val DATO_OPPDRAG_GJELDER_FOM = LocalDate.EPOCH.toXMLDate()
    val oppdragsenhet =
        OppdragsEnhet120().apply {
            typeEnhet = "BOS"
            enhet = "4819"
            datoEnhetFom = LocalDate.parse("1900-01-01").toXMLDate()
        }
    const val AKSJONSKODE_OPPDATER = "1"
    val TILLEGG = TfradragTillegg.T
}

object OppdragslinjeDefaults {
    const val SAKSBEHANDLER_ID = ""
    val FRADRAG_ELLER_TILLEGG = TfradragTillegg.T
    const val UTBETALINGSFREKVENS = "MND"
    const val KJOEREPLAN_SAMMEN_MED_NESTE_PLANLAGTE_UTBETALING = "J"
}