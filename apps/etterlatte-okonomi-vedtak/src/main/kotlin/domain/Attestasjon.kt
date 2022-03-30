package no.nav.etterlatte.domain

import java.time.LocalDate

data class Attestasjon(
    val attestantId: String, // attestant (funksjonaer-id) maks 8 tegn
    val ugyldigFraDato: LocalDate? = null // evt. dato for naar attestasjon ikke lenger er gyldig
)
