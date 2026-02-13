package no.nav.etterlatte.utbetaling.common

import no.trygdeetaten.skjema.oppdrag.OppdragsEnhet120
import no.trygdeetaten.skjema.oppdrag.TfradragTillegg
import java.time.LocalDate
import java.time.Month

object OppdragDefaults {
    const val AVLEVERENDE_KOMPONENTKODE = "ETTERLAT" // felles for alle etterlatteytelser

    // https://nav-it.slack.com/archives/C03793FA1EE/p1649233182935959
    const val MOTTAKENDE_KOMPONENTKODE = "OS" // Oppdragsystemet
    const val SAKSBEHANDLER_ID_SYSTEM_ETTERLATTEYTELSER = "EY" // Placeholder for å identifisere
    const val UTBETALINGSFREKVENS = "MND"
    const val AKSJONSKODE_OPPDATER = "1"
    val DATO_OPPDRAG_GJELDER_FOM = LocalDate.EPOCH.toXMLDate()
    val OPPDRAGSENHET_DATO_FOM: LocalDate = LocalDate.of(1900, Month.JANUARY, 1)
    val OPPDRAGSENHET =
        OppdragsEnhet120().apply {
            typeEnhet = "BOS"
            enhet = "4819"
            datoEnhetFom = OPPDRAGSENHET_DATO_FOM.toXMLDate()
        }
    val TILLEGG = TfradragTillegg.T
}

object OppdragslinjeDefaults {
    const val UTBETALINGSFREKVENS = "MND"
    val FRADRAG_ELLER_TILLEGG = TfradragTillegg.T
}
