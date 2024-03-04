package no.nav.etterlatte.behandling.domain

// TODO: flytt denne classen
data class SaksbehandlerEnhet(
    val enhetsNummer: String,
    val navn: String,
)

data class SaksbehandlerTema(
    val kode: String,
)
