package no.nav.etterlatte.trygdetid.avtale

import java.util.*

data class TrygdeavtaleRequest(
    val id: UUID?,
    val avtaleKode: String,
    val avtaleDatoKode: String?,
    val avtaleKriteriaKode: String?
)