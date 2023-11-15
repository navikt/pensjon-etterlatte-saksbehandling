package no.nav.etterlatte.brev.adresse

import no.nav.pensjon.brevbaker.api.model.Telefonnummer

data class Avsender(
    val kontor: String,
    val adresse: String,
    val postnummer: String,
    val telefonnummer: Telefonnummer,
    val saksbehandler: String?,
    val attestant: String?,
)
