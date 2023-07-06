package no.nav.etterlatte.brev.model

import no.nav.pensjon.brevbaker.api.model.Telefonnummer

abstract class BrevData {
    open fun flett(text: String?) = text
}

data class Avsender(
    val kontor: String,
    val adresse: String,
    val postnummer: String,
    val telefonnummer: Telefonnummer,
    val saksbehandler: String,
    val attestant: String?
)